package com.praxis.mdp.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchInfrastructureConfig {

  @Bean
  @ConditionalOnMissingBean
  Clock mdpClock(MdpProperties props) {
    if (props.getScheduleZone() == null || props.getScheduleZone().isBlank()) {
      return Clock.systemDefaultZone();
    }
    return Clock.system(ZoneId.of(props.getScheduleZone()));
  }
}
