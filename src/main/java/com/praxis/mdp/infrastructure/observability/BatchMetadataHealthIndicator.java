package com.praxis.mdp.infrastructure.observability;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Confirms the Spring Batch metadata store is reachable. */
@Component
public class BatchMetadataHealthIndicator implements HealthIndicator {

  private final JobExplorer explorer;

  public BatchMetadataHealthIndicator(JobExplorer explorer) {
    this.explorer = explorer;
  }

  @Override
  public Health health() {
    try {
      return Health.up().withDetail("jobs", explorer.getJobNames()).build();
    } catch (RuntimeException e) {
      return Health.down(e).build();
    }
  }
}
