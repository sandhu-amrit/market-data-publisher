package com.praxis.mdp.run;

import com.praxis.mdp.pipeline.JobCatalog;
import com.praxis.mdp.pipeline.RunParameters;
import com.praxis.mdp.pipeline.ScheduledRunLauncher;
import com.praxis.mdp.run.internal.guard.ActiveRunGuard;
import com.praxis.mdp.run.internal.guard.RunExceptions.InvalidRunStateException;
import com.praxis.mdp.run.internal.guard.RunExceptions.RunAlreadyActiveException;
import com.praxis.mdp.run.internal.guard.RunExceptions.UnknownJobException;
import com.praxis.mdp.run.internal.guard.RunExceptions.UnknownRunException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.stereotype.Service;

@Service
public class RunService implements ScheduledRunLauncher {

  private static final Logger log = LoggerFactory.getLogger(RunService.class);
  private static final int LIST_LIMIT = 200;

  private final JobCatalog catalog;
  private final JobExplorer explorer;
  private final JobOperator jobOperator;
  private final TaskExecutorJobLauncher asyncLauncher;
  private final ActiveRunGuard guard;

  public RunService(
      JobCatalog catalog,
      JobExplorer explorer,
      JobOperator jobOperator,
      TaskExecutorJobLauncher asyncLauncher,
      ActiveRunGuard guard) {
    this.catalog = catalog;
    this.explorer = explorer;
    this.jobOperator = jobOperator;
    this.asyncLauncher = asyncLauncher;
    this.guard = guard;
  }

  @Override
  public void launchScheduled(String jobName, LocalDate businessDate) {
    try {
      RunStatus status = launch(jobName, businessDate, "scheduler", "SCHEDULER");
      log.info(
          "Scheduled run {} of {} for {} accepted", status.executionId(), jobName, businessDate);
    } catch (RunAlreadyActiveException e) {
      log.info("Skipping scheduled run of {} for {}: {}", jobName, businessDate, e.getMessage());
    }
  }

  public RunStatus launch(
      String jobName, LocalDate businessDate, String triggeredBy, String triggerSource) {
    if (catalog.find(jobName).isEmpty()) {
      throw new UnknownJobException(jobName);
    }
    synchronized (guard) {
      guard.assertNoActiveRun(jobName, businessDate);
      long sequence = guard.nextSequence(jobName, businessDate);
      try {
        JobExecution execution =
            asyncLauncher.run(
                catalog.requireJob(jobName),
                RunParameters.of(businessDate, sequence, triggeredBy, triggerSource));
        log.info(
            "Accepted run {} of {} for {} (runSequence={}, triggeredBy={}, via {})",
            execution.getId(),
            jobName,
            businessDate,
            sequence,
            triggeredBy,
            triggerSource);
        return RunStatus.from(execution);
      } catch (JobExecutionAlreadyRunningException e) {
        throw new RunAlreadyActiveException(
            "A run of " + jobName + " for " + businessDate + " is already running");
      } catch (Exception e) {
        throw new IllegalStateException("Could not launch " + jobName, e);
      }
    }
  }

  public RunStatus launchAndWait(
      String jobName, LocalDate businessDate, String triggeredBy, String triggerSource)
      throws InterruptedException {
    RunStatus accepted = launch(jobName, businessDate, triggeredBy, triggerSource);
    while (true) {
      JobExecution execution = explorer.getJobExecution(accepted.executionId());
      if (execution == null || !execution.isRunning()) {
        return execution == null ? accepted : RunStatus.from(execution);
      }
      Thread.sleep(200);
    }
  }

  public Optional<RunStatus> find(long executionId) {
    JobExecution execution = explorer.getJobExecution(executionId);
    return execution == null ? Optional.empty() : Optional.of(RunStatus.from(execution));
  }

  public List<RunStatus> list(String jobName, LocalDate businessDate, BatchStatus status) {
    if (jobName != null && catalog.find(jobName).isEmpty()) {
      throw new UnknownJobException(jobName);
    }
    List<String> names =
        jobName == null ? catalog.all().stream().map(def -> def.name()).toList() : List.of(jobName);
    List<RunStatus> result = new ArrayList<>();
    for (String name : names) {
      int start = 0;
      while (result.size() < LIST_LIMIT && start < 2000) {
        List<JobInstance> instances = explorer.getJobInstances(name, start, 100);
        if (instances.isEmpty()) {
          break;
        }
        for (JobInstance instance : instances) {
          for (JobExecution execution : explorer.getJobExecutions(instance)) {
            if (businessDate != null
                && !businessDate.equals(RunParameters.businessDate(execution.getJobParameters()))) {
              continue;
            }
            if (status != null && execution.getStatus() != status) {
              continue;
            }
            result.add(RunStatus.from(execution));
            if (result.size() >= LIST_LIMIT) {
              break;
            }
          }
        }
        start += instances.size();
        if (instances.size() < 100) {
          break;
        }
      }
    }
    result.sort(Comparator.comparingLong(RunStatus::executionId).reversed());
    return result;
  }

  public RunStatus restart(long executionId) {
    JobExecution existing = require(executionId);
    if (existing.isRunning()) {
      throw new RunAlreadyActiveException(
          "Run " + executionId + " is still " + existing.getStatus());
    }
    if (existing.getStatus() != BatchStatus.FAILED && existing.getStatus() != BatchStatus.STOPPED) {
      throw new InvalidRunStateException(
          "Run " + executionId + " is " + existing.getStatus() + " and cannot be restarted");
    }
    try {
      long restarted = jobOperator.restart(executionId);
      JobExecution execution = explorer.getJobExecution(restarted);
      if (execution == null) {
        throw new IllegalStateException(
            "Restart of " + executionId + " did not produce an execution");
      }
      return RunStatus.from(execution);
    } catch (RunAlreadyActiveException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRunStateException(
          "Cannot restart run " + executionId + ": " + e.getMessage());
    }
  }

  public RunStatus stop(long executionId) {
    JobExecution existing = require(executionId);
    if (!existing.isRunning()) {
      throw new InvalidRunStateException(
          "Run " + executionId + " is " + existing.getStatus() + " and cannot be stopped");
    }
    try {
      jobOperator.stop(executionId);
    } catch (NoSuchJobExecutionException | JobExecutionNotRunningException e) {
      throw new InvalidRunStateException("Cannot stop run " + executionId + ": " + e.getMessage());
    }
    JobExecution updated = explorer.getJobExecution(executionId);
    return RunStatus.from(updated == null ? existing : updated);
  }

  private JobExecution require(long executionId) {
    JobExecution execution = explorer.getJobExecution(executionId);
    if (execution == null) {
      throw new UnknownRunException(executionId);
    }
    return execution;
  }
}
