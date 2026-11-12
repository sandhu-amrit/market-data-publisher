package com.praxis.mdp.source;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import java.time.LocalDate;
import org.springframework.batch.item.ItemStreamReader;

/**
 * Where a full end-of-day snapshot comes from. The file adapter is the default; an Asset Control
 * adapter can replace it without changing jobs.
 */
public interface MarketDataSource {

  /** Reader over the full snapshot of one instrument type on one business date. */
  ItemStreamReader<MarketDataRecord> reader(InstrumentType type, LocalDate businessDate);
}
