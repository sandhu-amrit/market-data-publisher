package com.praxis.mdp.pipeline.internal.jobs;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.policy.FailurePolicy;
import com.praxis.mdp.domain.policy.MissingPricePolicy;
import com.praxis.mdp.infrastructure.config.Connections;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.internal.flow.EodJobFactory;
import com.praxis.mdp.publishing.FileDestination;
import com.praxis.mdp.publishing.Format;
import com.praxis.mdp.publishing.SftpDestination;
import com.praxis.mdp.source.AssetControlSource;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** CME futures. On-demand. Publishes a local file and the internal SFTP profile. */
@Configuration
public class CmeFuturesJob {

  public static final String NAME = "cmeFuturesEodJob";

  @Bean
  JobDefinition cmeFuturesEodJob(AssetControlSource source, Connections connections) {
    return JobDefinition.builder(NAME)
        .instrumentType(InstrumentType.CME_FUTURE)
        .source(source)
        .missingPrices(MissingPricePolicy.SKIP_AND_ALERT)
        .onFailure(FailurePolicy.FAIL_FAST)
        .publishTo(
            FileDestination.local().fileName("{type}_EOD_{businessDate}.csv").format(Format.CSV))
        .publishTo(
            SftpDestination.to(connections.sftp().internal())
                .remoteDir("/inbound/cme")
                .fileName("cme_{businessDate}_{runSequence}.csv")
                .format(Format.CSV))
        .build();
  }

  @Bean
  Job cmeFuturesEodBatchJob(EodJobFactory factory, JobDefinition cmeFuturesEodJob) {
    return factory.create(cmeFuturesEodJob);
  }
}
