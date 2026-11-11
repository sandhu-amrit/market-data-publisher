package com.praxis.mdp.config;

import com.praxis.mdp.batch.EodJobFactory;
import com.praxis.mdp.model.InstrumentType;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Every job is defined here, in Java. The bean name is the job name used by the run API.
 * To add a job for another instrument type, add one more bean, e.g.:
 * {@code @Bean Job fxForwardsEodJob(EodJobFactory f) { return f.create("fxForwardsEodJob", InstrumentType.FX_FORWARD); }}
 */
@Configuration
public class JobsConfig {

    @Bean
    Job fxFuturesEodJob(EodJobFactory factory) {
        return factory.create("fxFuturesEodJob", InstrumentType.FX_FUTURE);
    }
}
