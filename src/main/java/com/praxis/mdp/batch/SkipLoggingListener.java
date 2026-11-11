package com.praxis.mdp.batch;

import com.praxis.mdp.model.MarketDataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;

/** Logs every skipped record (the run id, job and date come from the MDC). Counts are in the step execution. */
public class SkipLoggingListener implements SkipListener<MarketDataRecord, MarketDataRecord> {

    private static final Logger log = LoggerFactory.getLogger(SkipLoggingListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Skipped unreadable input line: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(MarketDataRecord item, Throwable t) {
        log.warn("Skipped invalid record: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(MarketDataRecord item, Throwable t) {
        log.warn("Skipped record that could not be written ({}): {}", item.instrument().id(), t.getMessage());
    }
}
