package com.praxis.mdp.publishing;

import com.praxis.mdp.domain.MarketDataRecord;
import org.springframework.batch.item.ItemStreamWriter;

/** Chunk writer that publishes (or discards) its output when the step ends. */
public interface SinkWriter extends ItemStreamWriter<MarketDataRecord> {

  /**
   * Called once when the step ends. On success the output becomes visible. On failure it is
   * discarded. Must be safe to call more than once and must close resources.
   */
  void finish(boolean success);
}
