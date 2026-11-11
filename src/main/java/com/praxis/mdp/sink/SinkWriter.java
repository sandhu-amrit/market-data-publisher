package com.praxis.mdp.sink;

import com.praxis.mdp.model.MarketDataRecord;
import org.springframework.batch.item.ItemStreamWriter;

/** A chunk writer that also knows how to publish (or discard) its output once the step has ended. */
public interface SinkWriter extends ItemStreamWriter<MarketDataRecord> {

    /**
     * Called once when the step ends. On success the output becomes visible to consumers; on failure it is
     * discarded. Must also close any open resources and be safe to call more than once.
     */
    void finish(boolean success);
}
