package com.praxis.mdp.source;

import com.praxis.mdp.model.InstrumentType;
import com.praxis.mdp.model.MarketDataRecord;
import java.time.LocalDate;
import org.springframework.batch.item.ItemStreamReader;

/**
 * Where market data comes from. v1 reads CSV files ({@link FileMarketDataSource}); a real Asset Control
 * implementation can be added later without touching jobs.
 */
public interface MarketDataSource {

    /** Creates a reader for the full snapshot of one instrument type on one business date. */
    ItemStreamReader<MarketDataRecord> reader(InstrumentType type, LocalDate businessDate);
}
