package com.praxis.mdp.pipeline.internal.listener;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A crashed JVM leaves executions in STARTED, which blocks that job and date and cannot be
 * restarted. Mark them FAILED so an operator can restart them.
 */
@Component
@Order(0)
public class StartupRecovery implements ApplicationRunner {

  static final String REASON =
      "Marked FAILED on startup because the previous process exited while this execution was running. "
          + "Restart it with POST /api/v1/runs/{id}/restart.";

  private static final Logger log = LoggerFactory.getLogger(StartupRecovery.class);

  private final JobExplorer explorer;
  private final JobRepository repository;
  private final TransactionTemplate tx;

  public StartupRecovery(
      JobExplorer explorer,
      JobRepository repository,
      PlatformTransactionManager transactionManager) {
    this.explorer = explorer;
    this.repository = repository;
    this.tx = new TransactionTemplate(transactionManager);
  }

  @Override
  public void run(ApplicationArguments args) {
    recover();
  }

  public int recover() {
    int recovered = 0;
    for (String jobName : explorer.getJobNames()) {
      for (JobExecution running : explorer.findRunningJobExecutions(jobName)) {
        JobExecution execution = explorer.getJobExecution(running.getId());
        if (execution == null || !execution.isRunning()) {
          continue;
        }
        tx.executeWithoutResult(status -> markFailed(execution));
        recovered++;
        log.warn("Recovered interrupted execution {} of {} as FAILED", execution.getId(), jobName);
      }
    }
    return recovered;
  }

  private void markFailed(JobExecution execution) {
    LocalDateTime now = LocalDateTime.now();
    for (StepExecution step : execution.getStepExecutions()) {
      if (step.getStatus().isRunning()) {
        step.setStatus(BatchStatus.FAILED);
        step.setExitStatus(ExitStatus.FAILED.addExitDescription(REASON));
        step.setEndTime(now);
        repository.update(step);
      }
    }
    execution.setStatus(BatchStatus.FAILED);
    execution.setExitStatus(ExitStatus.FAILED.addExitDescription(REASON));
    execution.setEndTime(now);
    repository.update(execution);
  }
}
