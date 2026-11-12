package com.praxis.mdp.publishing;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.RunId;

/** Identity of the run a publication is writing. */
public record PublishRequest(String jobName, InstrumentType type, RunId runId, Format format) {}
