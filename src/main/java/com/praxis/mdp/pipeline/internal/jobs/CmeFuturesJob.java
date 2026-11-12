package com.praxis.mdp.pipeline.internal.jobs;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.internal.flow.EodJobFactory;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CME futures end-of-day. On-demand only unless a cron is configured and the schedule is enabled.
 */
@Configuration
public class CmeFuturesJob {

  public static final String NAME = "cmeFuturesEodJob";

  @Bean
  JobDefinition cmeFuturesEodJobDefinition(MdpProperties props) {
    return JobDefinitions.resolve(props, NAME, InstrumentType.CME_FUTURE, "", false);
  }

  @Bean(name = NAME)
  Job cmeFuturesEodJob(EodJobFactory factory, JobDefinition cmeFuturesEodJobDefinition) {
    return factory.create(cmeFuturesEodJobDefinition);
  }
}
