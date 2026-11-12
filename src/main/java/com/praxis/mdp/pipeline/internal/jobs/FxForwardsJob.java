package com.praxis.mdp.pipeline.internal.jobs;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.policy.FailurePolicy;
import com.praxis.mdp.domain.policy.MissingPricePolicy;
import com.praxis.mdp.infrastructure.config.Connections;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.internal.flow.EodJobFactory;
import com.praxis.mdp.publishing.FileDestination;
import com.praxis.mdp.publishing.Format;
import com.praxis.mdp.publishing.RestDestination;
import com.praxis.mdp.source.AssetControlSource;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** FX forwards. Publishes a local file and the risk API. Batch tuning stays on the defaults. */
@Configuration
public class FxForwardsJob {

  public static final String NAME = "fxForwardsEodJob";

  @Bean
  JobDefinition fxForwardsEodJob(AssetControlSource source, Connections connections) {
    return JobDefinition.builder(NAME)
        .instrumentType(InstrumentType.FX_FORWARD)
        .source(source)
        .missingPrices(MissingPricePolicy.SKIP_AND_ALERT)
        .onFailure(FailurePolicy.FAIL_FAST)
        .publishTo(
            FileDestination.local().fileName("{type}_EOD_{businessDate}.csv").format(Format.CSV))
        .publishTo(
            RestDestination.to(connections.rest().riskApi())
                .path("/api/fx-forwards")
                .format(Format.JSON))
        .build();
  }

  @Bean
  Job fxForwardsEodBatchJob(EodJobFactory factory, JobDefinition fxForwardsEodJob) {
    return factory.create(fxForwardsEodJob);
  }
}
