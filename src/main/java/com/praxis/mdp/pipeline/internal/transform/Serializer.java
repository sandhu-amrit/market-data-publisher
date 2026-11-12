package com.praxis.mdp.pipeline.internal.transform;

import com.praxis.mdp.domain.MarketDataRecord;

/** Line-oriented snapshot format used by the extract step. */
public interface Serializer {

  String format();

  String header();

  String serialize(MarketDataRecord record);
}
