package com.praxis.mdp.source;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import java.time.LocalDate;
import org.springframework.batch.item.ItemStreamReader;

/**
 * Where a full end-of-day snapshot comes from. {@code fetchSize} is the page size for adapters that
 * read in pages. The file adapter reads the whole file.
 */
public interface MarketDataSource {

  ItemStreamReader<MarketDataRecord> reader(
      InstrumentType type, LocalDate businessDate, int fetchSize);
}
