package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.policy.FailurePolicy;
import com.praxis.mdp.domain.policy.MissingPricePolicy;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.RunParameters;
import com.praxis.mdp.pipeline.internal.flow.EodJobFactory;
import com.praxis.mdp.publishing.FileDestination;
import com.praxis.mdp.publishing.Format;
import com.praxis.mdp.publishing.Publication;
import com.praxis.mdp.publishing.PublishRequest;
import com.praxis.mdp.publishing.PublishingClients;
import com.praxis.mdp.publishing.SinkWriter;
import com.praxis.mdp.source.AssetControlSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class ContinuePolicyTest {

  private static final AtomicInteger RECORDED = new AtomicInteger();
  private static final Path OUTPUT = TestDirs.newTempDir("mdp-cont-out");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("mdp.output-dir", OUTPUT::toString);
    registry.add("mdp.work-dir", () -> TestDirs.newTempDir("mdp-cont-work").toString());
  }

  @Autowired TaskExecutorJobLauncher launcher;
  @Autowired JobExplorer explorer;
  @Autowired EodJobFactory factory;
  @Autowired AssetControlSource source;

  @Test
  void laterDestinationsStillRunAndTheJobFails() throws Exception {
    RECORDED.set(0);
    var job =
        factory.create(
            JobDefinition.builder("continuePolicyJob")
                .instrumentType(InstrumentType.FX_FUTURE)
                .source(source)
                .missingPrices(MissingPricePolicy.SKIP_AND_ALERT)
                .onFailure(FailurePolicy.CONTINUE_OTHER_DESTINATIONS)
                .publishTo(
                    FileDestination.local()
                        .fileName("{type}_EOD_{businessDate}.csv")
                        .format(Format.CSV))
                .publishTo(
                    scripted(
                        "failing",
                        () -> {
                          throw new IllegalStateException("destination down");
                        }))
                .publishTo(scripted("recording", RECORDED::incrementAndGet))
                .build());
    var started =
        launcher.run(job, RunParameters.of(LocalDate.parse("2026-11-11"), 1, "test", "TEST"));
    await()
        .atMost(Duration.ofSeconds(20))
        .until(() -> !explorer.getJobExecution(started.getId()).isRunning());
    var execution = explorer.getJobExecution(started.getId());
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(RECORDED.get()).isGreaterThan(0);
    assertThat(Files.exists(OUTPUT.resolve("FX_FUTURE_EOD_2026-11-11.csv"))).isTrue();
  }

  private static Publication scripted(String name, Runnable onWrite) {
    return new Publication() {
      @Override
      public String stepName() {
        return name;
      }

      @Override
      public Format format() {
        return Format.CSV;
      }

      @Override
      public boolean active() {
        return true;
      }

      @Override
      public SinkWriter open(PublishRequest request, PublishingClients clients) {
        return new SinkWriter() {
          @Override
          public void open(ExecutionContext executionContext) {}

          @Override
          public void write(Chunk<? extends MarketDataRecord> chunk) {
            onWrite.run();
          }

          @Override
          public void update(ExecutionContext executionContext) {}

          @Override
          public void close() {}

          @Override
          public void finish(boolean success) {}
        };
      }
    };
  }
}
