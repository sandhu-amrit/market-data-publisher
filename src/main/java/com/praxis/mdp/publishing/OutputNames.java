package com.praxis.mdp.publishing;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.RunId;

/** Substitutes {@code {type}}, {@code {job}}, {@code {businessDate}}, and {@code {runSequence}}. */
public final class OutputNames {

  private OutputNames() {}

  public static String apply(String pattern, String jobName, InstrumentType type, RunId runId) {
    return pattern
        .replace("{job}", jobName)
        .replace("{type}", type.name())
        .replace("{businessDate}", runId.businessDate().toString())
        .replace("{date}", runId.businessDate().toString())
        .replace("{runSequence}", Long.toString(runId.runSequence()));
  }
}
