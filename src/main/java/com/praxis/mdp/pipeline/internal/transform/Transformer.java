package com.praxis.mdp.pipeline.internal.transform;

import com.praxis.mdp.domain.MarketDataRecord;

/** Optional enrichment applied before each destination write. The default is identity. */
public interface Transformer {

  MarketDataRecord transform(MarketDataRecord record);
}
