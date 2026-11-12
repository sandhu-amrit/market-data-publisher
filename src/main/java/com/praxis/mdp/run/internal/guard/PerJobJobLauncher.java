package com.praxis.mdp.run.internal.guard;

import com.praxis.mdp.infrastructure.config.JobExecutors;
import com.praxis.mdp.pipeline.JobRuntime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;

/** Routes each launch onto that job's own pool. */
public class PerJobJobLauncher extends TaskExecutorJobLauncher {

  private final JobRepository repository;
  private final JobExecutors executors;
  private final JobRuntime runtime;
  private final ConcurrentMap<String, TaskExecutorJobLauncher> launchers =
      new ConcurrentHashMap<>();

  public PerJobJobLauncher(JobRepository repository, JobExecutors executors, JobRuntime runtime) {
    this.repository = repository;
    this.executors = executors;
    this.runtime = runtime;
  }

  @Override
  public JobExecution run(Job job, JobParameters jobParameters)
      throws JobExecutionAlreadyRunningException,
          JobRestartException,
          JobInstanceAlreadyCompleteException,
          JobParametersInvalidException {
    return launcher(job.getName()).run(job, jobParameters);
  }

  private TaskExecutorJobLauncher launcher(String jobName) {
    return launchers.computeIfAbsent(
        jobName,
        name -> {
          TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
          launcher.setJobRepository(repository);
          launcher.setTaskExecutor(executors.forJob(name, runtime.batch(name).threads()));
          try {
            launcher.afterPropertiesSet();
          } catch (Exception e) {
            throw new IllegalStateException("Cannot create launcher for " + name, e);
          }
          return launcher;
        });
  }
}
