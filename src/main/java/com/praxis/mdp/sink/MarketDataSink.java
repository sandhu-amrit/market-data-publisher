package com.praxis.mdp.sink;

import com.praxis.mdp.model.InstrumentType;
import java.time.LocalDate;

/**
 * A destination for market data. v1 has {@link FileMarketDataSink}; Kafka, SFTP and REST sinks implement
 * the same interface and get their own step in a job.
 */
public interface MarketDataSink {

    /** Creates a writer for one run's output. */
    SinkWriter writer(InstrumentType type, LocalDate businessDate);
}
