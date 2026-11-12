package com.praxis.mdp.pipeline.internal.jobs;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.internal.flow.EodJobFactory;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** FX futures end-of-day. Default schedule is 16:00 on weekdays. */
@Configuration
public class FxFuturesJob {

  public static final String NAME = "fxFuturesEodJob";

  @Bean
  JobDefinition fxFuturesEodJobDefinition(MdpProperties props) {
    return JobDefinitions.resolve(
        props, NAME, InstrumentType.FX_FUTURE, "0 0 16 * * MON-FRI", true);
  }

  @Bean(name = NAME)
  Job fxFuturesEodJob(EodJobFactory factory, JobDefinition fxFuturesEodJobDefinition) {
    return factory.create(fxFuturesEodJobDefinition);
  }
}
