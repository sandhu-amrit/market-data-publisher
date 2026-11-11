package com.praxis.mdp.api;

import com.praxis.mdp.batch.RunParameters;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Launches jobs asynchronously and answers status queries. A run is identified by job + businessDate +
 * runSequence; the sequence is computed here (1, 2, 3 ... per job and date).
 */
@Service
public class RunService {

    private static final Logger log = LoggerFactory.getLogger(RunService.class);
    /** How many of a job's most recent instances are scanned when looking for same-date runs. */
    private static final int HISTORY_WINDOW = 1000;

    private final Map<String, Job> jobs;
    private final JobExplorer explorer;
    private final TaskExecutorJobLauncher asyncLauncher;

    public RunService(Map<String, Job> jobs, JobRepository jobRepository, JobExplorer explorer) throws Exception {
        this.jobs = jobs;
        this.explorer = explorer;
        this.asyncLauncher = new TaskExecutorJobLauncher();
        asyncLauncher.setJobRepository(jobRepository);
        asyncLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor("mdp-run-"));
        asyncLauncher.afterPropertiesSet();
    }

    /**
     * Starts a run and returns immediately. Synchronized so two requests in this process cannot both pass the
     * "already running" check; see the README for the multi-node caveat.
     */
    public synchronized JobExecution launch(String jobName, LocalDate businessDate, String triggeredBy, String triggerSource) {
        Job job = Optional.ofNullable(jobs.get(jobName)).orElseThrow(() -> new UnknownJobException(jobName));

        boolean running = explorer.findRunningJobExecutions(jobName).stream()
                .anyMatch(je -> businessDate.equals(RunParameters.businessDate(je.getJobParameters())));
        if (running) {
            throw new RunAlreadyActiveException("A run of " + jobName + " for " + businessDate + " is already running");
        }

        long sequence = nextSequence(jobName, businessDate);
        try {
            JobExecution je = asyncLauncher.run(job, RunParameters.of(businessDate, sequence, triggeredBy, triggerSource));
            log.info("Accepted run {} of {} for {} (runSequence={}, triggeredBy={}, via {})",
                    je.getId(), jobName, businessDate, sequence, triggeredBy, triggerSource);
            return je;
        } catch (Exception e) {
            throw new IllegalStateException("Could not launch " + jobName, e);
        }
    }

    public Optional<JobExecution> find(long executionId) {
        return Optional.ofNullable(explorer.getJobExecution(executionId));
    }

    private long nextSequence(String jobName, LocalDate businessDate) {
        long max = 0;
        for (JobInstance instance : explorer.getJobInstances(jobName, 0, HISTORY_WINDOW)) {
            for (JobExecution je : explorer.getJobExecutions(instance)) {
                if (businessDate.equals(RunParameters.businessDate(je.getJobParameters()))) {
                    Long seq = je.getJobParameters().getLong(RunParameters.RUN_SEQUENCE);
                    if (seq != null) {
                        max = Math.max(max, seq);
                    }
                }
            }
        }
        return max + 1;
    }
}
