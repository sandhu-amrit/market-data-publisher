package com.praxis.mdp.pipeline.internal.listener;

import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.infrastructure.observability.PublishMetrics;
import com.praxis.mdp.infrastructure.observability.RunMdc;
import com.praxis.mdp.pipeline.internal.validate.MissingPriceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Component
public class SkipLoggingListener implements SkipListener<MarketDataRecord, MarketDataRecord> {

  private static final Logger log = LoggerFactory.getLogger(SkipLoggingListener.class);

  private final PublishMetrics metrics;

  public SkipLoggingListener(PublishMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public void onSkipInRead(Throwable t) {
    log.warn("Skipped unreadable input line: {}", t.getMessage());
    metrics.recordsSkipped(RunMdc.jobName(), "parse");
  }

  @Override
  public void onSkipInProcess(MarketDataRecord item, Throwable t) {
    if (t instanceof MissingPriceException) {
      log.error("ALERT missing EOD price for instrument {}", item.instrument().id());
      metrics.recordsSkipped(RunMdc.jobName(), "missing_price");
      return;
    }
    log.warn("Skipped invalid record: {}", t.getMessage());
    metrics.recordsSkipped(RunMdc.jobName(), "invalid");
  }

  @Override
  public void onSkipInWrite(MarketDataRecord item, Throwable t) {
    log.warn(
        "Skipped record that could not be written ({}): {}",
        item.instrument().id(),
        t.getMessage());
    metrics.recordsSkipped(RunMdc.jobName(), "write");
  }
}
