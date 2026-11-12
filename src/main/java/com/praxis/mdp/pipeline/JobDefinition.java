package com.praxis.mdp.pipeline;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.policy.FailurePolicy;
import com.praxis.mdp.domain.policy.MissingPricePolicy;
import java.util.List;

/**
 * One end-of-day job. The shape (instrument type, steps) is defined in Java. Cron, policies, and
 * destination filters come from configuration, with the defaults declared next to the job.
 */
public record JobDefinition(
    String name,
    InstrumentType instrumentType,
    FailurePolicy failurePolicy,
    MissingPricePolicy missingPricePolicy,
    List<String> destinations,
    String cron,
    boolean scheduleEnabled) {

  public JobDefinition {
    destinations = destinations == null ? List.of() : List.copyOf(destinations);
  }
}
