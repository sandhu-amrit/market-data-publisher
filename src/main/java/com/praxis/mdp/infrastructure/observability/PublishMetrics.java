package com.praxis.mdp.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Counters and timers for extract and per-destination publish. */
@Component
public class PublishMetrics {

  private final MeterRegistry registry;

  public PublishMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void recordsRead(String job, long count) {
    if (count > 0) {
      registry.counter("mdp.records.read", "job", job).increment(count);
    }
  }

  public void recordsWritten(String job, String destination, long count) {
    if (count > 0) {
      registry
          .counter("mdp.records.written", "job", job, "destination", destination)
          .increment(count);
    }
  }

  public void recordsSkipped(String job, String reason) {
    registry.counter("mdp.records.skipped", "job", job, "reason", reason).increment();
  }

  public void destinationFailure(String job, String destination) {
    registry
        .counter("mdp.destination.failures", "job", job, "destination", destination)
        .increment();
  }

  public void destinationDuration(String job, String destination, Duration duration) {
    Timer.builder("mdp.destination.duration")
        .tag("job", job)
        .tag("destination", destination)
        .register(registry)
        .record(duration);
  }
}
