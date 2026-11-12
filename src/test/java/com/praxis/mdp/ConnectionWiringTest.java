package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;

import com.praxis.mdp.infrastructure.config.JobExecutors;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.JobRuntime;
import com.praxis.mdp.publishing.KafkaDestination;
import com.praxis.mdp.publishing.RestDestination;
import com.praxis.mdp.publishing.SftpDestination;
import com.praxis.mdp.publishing.internal.kafka.KafkaProducers;
import com.praxis.mdp.publishing.internal.rest.RestClients;
import com.praxis.mdp.publishing.internal.sftp.SftpSessions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootTest
class ConnectionWiringTest {

  @Autowired JobDefinition fxFuturesEodJob;
  @Autowired JobDefinition fxForwardsEodJob;
  @Autowired JobDefinition cmeFuturesEodJob;
  @Autowired SftpSessions sftpSessions;
  @Autowired KafkaProducers kafkaProducers;
  @Autowired RestClients restClients;
  @Autowired JobRuntime runtime;
  @Autowired JobExecutors executors;

  @Test
  void futuresAndCmeUseDifferentSftpProfiles() {
    SftpDestination.Builder futures = sftp(fxFuturesEodJob);
    SftpDestination.Builder cme = sftp(cmeFuturesEodJob);
    assertThat(futures.connection().host()).isEqualTo("sftp-vendor-a");
    assertThat(futures.connection().enabled()).isFalse();
    assertThat(cme.connection().host()).isEqualTo("sftp-internal");
    assertThat(cme.remoteDir()).isEqualTo("/inbound/cme");
    assertThat(futures.connection()).isNotEqualTo(cme.connection());
  }

  @Test
  void forwardsUsesTheRiskApiAndFuturesUsesMainKafka() {
    RestDestination.Builder rest =
        fxForwardsEodJob.publications().stream()
            .filter(RestDestination.Builder.class::isInstance)
            .map(RestDestination.Builder.class::cast)
            .findFirst()
            .orElseThrow();
    KafkaDestination.Builder kafka =
        fxFuturesEodJob.publications().stream()
            .filter(KafkaDestination.Builder.class::isInstance)
            .map(KafkaDestination.Builder.class::cast)
            .findFirst()
            .orElseThrow();
    assertThat(rest.path()).isEqualTo("/api/fx-forwards");
    assertThat(rest.connection().baseUrl()).contains("8081");
    assertThat(kafka.topic()).isEqualTo("mdp.fx-futures.eod");
    assertThat(kafka.connection().bootstrapServers()).isEqualTo("localhost:9092");
    assertThat(runtime.batch("fxFuturesEodJob").chunkSize()).isEqualTo(2000);
    assertThat(runtime.batch("fxFuturesEodJob").threads()).isEqualTo(4);
    assertThat(runtime.batch("fxForwardsEodJob").chunkSize()).isEqualTo(1000);
    assertThat(runtime.batch("cmeFuturesEodJob").threads()).isEqualTo(3);
    ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executors.require("fxFuturesEodJob");
    assertThat(pool.getCorePoolSize()).isEqualTo(4);
    assertThat(pool.getThreadNamePrefix()).isEqualTo("mdp-fxFuturesEodJob-");
  }

  @Test
  void disabledProfilesAreNotConnectedAtStartup() {
    assertThat(sftpSessions.size()).isZero();
    assertThat(kafkaProducers.size()).isZero();
    assertThat(restClients.size()).isZero();
  }

  private static SftpDestination.Builder sftp(JobDefinition job) {
    return job.publications().stream()
        .filter(SftpDestination.Builder.class::isInstance)
        .map(SftpDestination.Builder.class::cast)
        .findFirst()
        .orElseThrow();
  }
}
