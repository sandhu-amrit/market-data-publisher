package com.praxis.mdp.pipeline.internal.jobs;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.policy.FailurePolicy;
import com.praxis.mdp.domain.policy.MissingPricePolicy;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.pipeline.JobDefinition;
import java.util.List;

/** Java defaults for a job, overridden field-by-field from {@code mdp.jobs.<name>}. */
final class JobDefinitions {

  private JobDefinitions() {}

  static JobDefinition resolve(
      MdpProperties props,
      String name,
      InstrumentType type,
      String defaultCron,
      boolean defaultScheduleEnabled) {
    MdpProperties.JobConfig cfg = props.getJobs().get(name);
    if (cfg == null) {
      return new JobDefinition(
          name,
          type,
          FailurePolicy.FAIL_FAST,
          MissingPricePolicy.SKIP_AND_ALERT,
          List.of(),
          defaultCron,
          defaultScheduleEnabled);
    }
    List<String> destinations = cfg.getDestinations() == null ? List.of() : cfg.getDestinations();
    return new JobDefinition(
        name,
        type,
        cfg.getFailurePolicy() != null ? cfg.getFailurePolicy() : FailurePolicy.FAIL_FAST,
        cfg.getMissingPricePolicy() != null
            ? cfg.getMissingPricePolicy()
            : MissingPricePolicy.SKIP_AND_ALERT,
        destinations,
        cfg.getCron() != null ? cfg.getCron() : defaultCron,
        cfg.getScheduleEnabled() != null ? cfg.getScheduleEnabled() : defaultScheduleEnabled);
  }
}
