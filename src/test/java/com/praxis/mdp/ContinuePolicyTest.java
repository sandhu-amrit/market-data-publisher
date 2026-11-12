package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.praxis.mdp.pipeline.RunParameters;
import com.praxis.mdp.publishing.Destination;
import com.praxis.mdp.support.ScriptedDestination;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Import(ContinuePolicyTest.ExtraDestinations.class)
class ContinuePolicyTest {

  static final AtomicInteger RECORDED = new AtomicInteger();
  private static final Path OUTPUT = TestDirs.newTempDir("mdp-cont-out");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("mdp.output-dir", OUTPUT::toString);
    registry.add("mdp.work-dir", () -> TestDirs.newTempDir("mdp-cont-work").toString());
    registry.add("mdp.jobs.fxFuturesEodJob.failure-policy", () -> "CONTINUE");
  }

  @Autowired TaskExecutorJobLauncher launcher;

  @Autowired JobExplorer explorer;

  @Autowired
  @Qualifier("fxFuturesEodJob")
  Job job;

  @Test
  void laterDestinationsStillRunAndTheJobFails() throws Exception {
    RECORDED.set(0);
    var started =
        launcher.run(job, RunParameters.of(LocalDate.parse("2026-11-11"), 1, "test", "TEST"));
    await()
        .atMost(Duration.ofSeconds(20))
        .until(() -> !explorer.getJobExecution(started.getId()).isRunning());
    var execution = explorer.getJobExecution(started.getId());
    assertThat(execution.getStepExecutions())
        .extracting(org.springframework.batch.core.StepExecution::getStepName)
        .as("recorded=%s status=%s", RECORDED.get(), execution.getStatus())
        .contains("publish-failing", "publish-recording", "failureGate");
    assertThat(execution.getStatus())
        .as(
            "recorded=%s steps=%s",
            RECORDED.get(),
            execution.getStepExecutions().stream()
                .map(
                    step ->
                        step.getStepName() + "=" + step.getStatus() + "/w" + step.getWriteCount())
                .toList())
        .isEqualTo(BatchStatus.FAILED);
    assertThat(RECORDED.get()).isGreaterThan(0);
    assertThat(Files.exists(OUTPUT.resolve("FX_FUTURE_EOD_2026-11-11.csv"))).isTrue();
    assertThat(
            execution.getStepExecutions().stream()
                .anyMatch(step -> "failureGate".equals(step.getStepName())))
        .isTrue();
  }

  @TestConfiguration
  static class ExtraDestinations {
    @Bean
    Destination failingDestination() {
      return new ScriptedDestination(
          "failing",
          20,
          () -> {
            throw new IllegalStateException("destination down");
          });
    }

    @Bean
    Destination recordingDestination() {
      return new ScriptedDestination("recording", 30, RECORDED::incrementAndGet);
    }
  }
}
