package com.praxis.mdp.publishing;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.RunId;

/** Writes one run's snapshot to a single downstream. */
public interface MarketDataSink {

  SinkWriter open(String jobName, InstrumentType type, RunId runId);
}
