package com.praxis.mdp.pipeline.internal.jobs;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.internal.flow.EodJobFactory;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** FX forwards end-of-day. Default schedule is 17:00 on weekdays. */
@Configuration
public class FxForwardsJob {

  public static final String NAME = "fxForwardsEodJob";

  @Bean
  JobDefinition fxForwardsEodJobDefinition(MdpProperties props) {
    return JobDefinitions.resolve(
        props, NAME, InstrumentType.FX_FORWARD, "0 0 17 * * MON-FRI", true);
  }

  @Bean(name = NAME)
  Job fxForwardsEodJob(EodJobFactory factory, JobDefinition fxForwardsEodJobDefinition) {
    return factory.create(fxForwardsEodJobDefinition);
  }
}
