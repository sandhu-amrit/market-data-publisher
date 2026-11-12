package com.praxis.mdp.pipeline.internal.jobs;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.policy.FailurePolicy;
import com.praxis.mdp.domain.policy.MissingPricePolicy;
import com.praxis.mdp.infrastructure.config.Connections;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.internal.flow.EodJobFactory;
import com.praxis.mdp.publishing.FileDestination;
import com.praxis.mdp.publishing.Format;
import com.praxis.mdp.publishing.KafkaDestination;
import com.praxis.mdp.publishing.SftpDestination;
import com.praxis.mdp.source.AssetControlSource;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** FX futures. Publishes a local file, SFTP vendor A, and the main Kafka profile. */
@Configuration
public class FxFuturesJob {

  public static final String NAME = "fxFuturesEodJob";

  @Bean
  JobDefinition fxFuturesEodJob(AssetControlSource source, Connections connections) {
    return JobDefinition.builder(NAME)
        .instrumentType(InstrumentType.FX_FUTURE)
        .source(source)
        .missingPrices(MissingPricePolicy.SKIP_AND_ALERT)
        .onFailure(FailurePolicy.CONTINUE_OTHER_DESTINATIONS)
        .publishTo(
            FileDestination.local().fileName("{type}_EOD_{businessDate}.csv").format(Format.CSV))
        .publishTo(
            SftpDestination.to(connections.sftp().vendorA())
                .remoteDir("/inbound/fx-futures")
                .fileName("fx_futures_{businessDate}_{runSequence}.csv")
                .format(Format.CSV))
        .publishTo(
            KafkaDestination.to(connections.kafka().main())
                .topic("mdp.fx-futures.eod")
                .format(Format.JSON))
        .build();
  }

  @Bean
  Job fxFuturesEodBatchJob(EodJobFactory factory, JobDefinition fxFuturesEodJob) {
    return factory.create(fxFuturesEodJob);
  }
}
