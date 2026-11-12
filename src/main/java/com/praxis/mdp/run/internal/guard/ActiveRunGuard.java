package com.praxis.mdp.run.internal.guard;

import com.praxis.mdp.pipeline.RunParameters;
import com.praxis.mdp.run.internal.guard.RunExceptions.RunAlreadyActiveException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Component;

/** Duplicate-run guard and runSequence allocation, both backed by Spring Batch's job repository. */
@Component
public class ActiveRunGuard {

  private static final Logger log = LoggerFactory.getLogger(ActiveRunGuard.class);
  private static final int PAGE = 200;
  private static final int MAX_INSTANCES = 5000;

  private final JobExplorer explorer;

  public ActiveRunGuard(JobExplorer explorer) {
    this.explorer = explorer;
  }

  public void assertNoActiveRun(String jobName, LocalDate businessDate) {
    boolean running =
        explorer.findRunningJobExecutions(jobName).stream()
            .anyMatch(
                execution ->
                    businessDate.equals(RunParameters.businessDate(execution.getJobParameters())));
    if (running) {
      throw new RunAlreadyActiveException(
          "A run of " + jobName + " for " + businessDate + " is already running");
    }
  }

  public long nextSequence(String jobName, LocalDate businessDate) {
    long max = 0;
    int start = 0;
    while (start < MAX_INSTANCES) {
      ListSlice slice = page(jobName, start);
      if (slice.instances().isEmpty()) {
        break;
      }
      for (JobInstance instance : slice.instances()) {
        for (JobExecution execution : explorer.getJobExecutions(instance)) {
          if (!businessDate.equals(safeDate(execution))) {
            continue;
          }
          Long sequence = execution.getJobParameters().getLong(RunParameters.RUN_SEQUENCE);
          if (sequence != null) {
            max = Math.max(max, sequence);
          }
        }
      }
      start += slice.instances().size();
      if (slice.instances().size() < PAGE) {
        break;
      }
      if (start >= MAX_INSTANCES) {
        log.warn(
            "Stopped scanning {} instances of {} while allocating runSequence",
            MAX_INSTANCES,
            jobName);
      }
    }
    return max + 1;
  }

  private ListSlice page(String jobName, int start) {
    return new ListSlice(explorer.getJobInstances(jobName, start, PAGE));
  }

  private static LocalDate safeDate(JobExecution execution) {
    try {
      return RunParameters.businessDate(execution.getJobParameters());
    } catch (RuntimeException e) {
      return null;
    }
  }

  private record ListSlice(java.util.List<JobInstance> instances) {}
}
