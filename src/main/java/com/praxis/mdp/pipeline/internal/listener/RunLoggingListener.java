package com.praxis.mdp.pipeline.internal.listener;

import com.praxis.mdp.infrastructure.observability.RunMdc;
import com.praxis.mdp.pipeline.RunParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class RunLoggingListener implements JobExecutionListener {

  private static final Logger log = LoggerFactory.getLogger(RunLoggingListener.class);

  @Override
  public void beforeJob(JobExecution jobExecution) {
    var params = jobExecution.getJobParameters();
    RunMdc.put(
        jobExecution.getJobInstance().getJobName(),
        params.getString(RunParameters.BUSINESS_DATE),
        String.valueOf(jobExecution.getId()),
        String.valueOf(params.getLong(RunParameters.RUN_SEQUENCE)),
        params.getString(RunParameters.TRIGGERED_BY));
    log.info(
        "Run started (runSequence={}, triggeredBy={}, triggerSource={})",
        params.getLong(RunParameters.RUN_SEQUENCE),
        params.getString(RunParameters.TRIGGERED_BY),
        params.getString(RunParameters.TRIGGER_SOURCE));
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    try {
      for (StepExecution step : jobExecution.getStepExecutions()) {
        log.info(
            "Step {}: {} read={} written={} skipped={}",
            step.getStepName(),
            step.getStatus(),
            step.getReadCount(),
            step.getWriteCount(),
            step.getSkipCount());
      }
      log.info("Run finished with status {}", jobExecution.getStatus());
      jobExecution
          .getAllFailureExceptions()
          .forEach(t -> log.error("Run failure: {}", t.toString()));
    } finally {
      RunMdc.clear();
    }
  }
}
