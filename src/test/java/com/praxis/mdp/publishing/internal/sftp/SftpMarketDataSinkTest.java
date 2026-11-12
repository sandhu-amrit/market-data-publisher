package com.praxis.mdp.publishing.internal.sftp;

import static org.assertj.core.api.Assertions.assertThat;

import com.praxis.mdp.TestDirs;
import com.praxis.mdp.domain.EodPrice;
import com.praxis.mdp.domain.Instrument;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.infrastructure.config.Connections.SftpConnection;
import com.praxis.mdp.publishing.SinkWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

class SftpMarketDataSinkTest {

  @Test
  @EnabledIf("dockerAvailable")
  void uploadsThenRenames() throws Exception {
    try (GenericContainer<?> sftp =
        new GenericContainer<>(DockerImageName.parse("atmoz/sftp:alpine"))
            .withCommand("mdp:mdp:::upload")
            .withExposedPorts(22)
            .waitingFor(Wait.forListeningPort())) {
      sftp.start();
      Path work = TestDirs.newTempDir("mdp-sftp-work");
      SftpConnection cfg =
          SftpConnection.open(sftp.getHost(), sftp.getMappedPort(22), "mdp", "mdp", true, true);

      SftpMarketDataSink sink =
          new SftpMarketDataSink(cfg, work, "upload", "{type}_EOD_{businessDate}.csv");
      MarketDataRecord record =
          new MarketDataRecord(
              new Instrument(
                  "FXF-9", InstrumentType.FX_FUTURE, "6EZ6", "USD", LocalDate.parse("2026-12-14")),
              new EodPrice(LocalDate.parse("2026-11-11"), new BigDecimal("1.5")));
      SinkWriter writer =
          sink.open(
              "fxFuturesEodJob",
              InstrumentType.FX_FUTURE,
              RunId.of(LocalDate.parse("2026-11-11"), 1));
      writer.open(new ExecutionContext());
      writer.write(new Chunk<>(List.of(record)));
      writer.finish(true);

      DefaultSftpSessionFactory sessions = new DefaultSftpSessionFactory();
      sessions.setHost(cfg.host());
      sessions.setPort(cfg.port());
      sessions.setUser("mdp");
      sessions.setPassword("mdp");
      sessions.setAllowUnknownKeys(true);
      try (SftpSession session = sessions.getSession()) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        session.read("upload/FX_FUTURE_EOD_2026-11-11.csv", out);
        assertThat(out.toString()).contains("FXF-9");
      }
    }
  }

  static boolean dockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable ignored) {
      return false;
    }
  }
}
