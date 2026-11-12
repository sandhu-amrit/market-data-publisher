package com.praxis.mdp.pipeline.internal.listener;

import com.praxis.mdp.infrastructure.observability.PublishMetrics;
import com.praxis.mdp.infrastructure.observability.RunMdc;
import java.time.Duration;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class MetricsStepListener implements StepExecutionListener {

  static final String EXTRACT = "extract";
  static final String PUBLISH_PREFIX = "publish-";

  private final PublishMetrics metrics;

  public MetricsStepListener(PublishMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    String job = RunMdc.jobName();
    String name = stepExecution.getStepName();
    if (EXTRACT.equals(name)) {
      metrics.recordsRead(job, stepExecution.getReadCount());
      return null;
    }
    if (name.startsWith(PUBLISH_PREFIX)) {
      String destination = name.substring(PUBLISH_PREFIX.length());
      metrics.recordsWritten(job, destination, stepExecution.getWriteCount());
      if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
        metrics.destinationDuration(
            job,
            destination,
            Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime()));
      }
      if (stepExecution.getStatus().isUnsuccessful()) {
        metrics.destinationFailure(job, destination);
      }
    }
    return null;
  }
}
