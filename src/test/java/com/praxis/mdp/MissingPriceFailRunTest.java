package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.praxis.mdp.pipeline.RunParameters;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class MissingPriceFailRunTest {

  private static final Path OUTPUT = TestDirs.newTempDir("mdp-fail-out");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("mdp.output-dir", OUTPUT::toString);
    registry.add("mdp.work-dir", () -> TestDirs.newTempDir("mdp-fail-work").toString());
    registry.add("mdp.jobs.fxFuturesEodJob.missing-price-policy", () -> "FAIL_RUN");
  }

  @Autowired TaskExecutorJobLauncher launcher;

  @Autowired JobExplorer explorer;

  @Autowired
  @Qualifier("fxFuturesEodJob")
  Job job;

  @Test
  void missingPriceFailsTheRun() throws Exception {
    JobExecution started =
        launcher.run(job, RunParameters.of(LocalDate.parse("2026-11-11"), 1, "test", "TEST"));
    await()
        .atMost(Duration.ofSeconds(20))
        .until(() -> !explorer.getJobExecution(started.getId()).isRunning());
    assertThat(explorer.getJobExecution(started.getId()).getStatus()).isEqualTo(BatchStatus.FAILED);
    try (var files = Files.list(OUTPUT)) {
      assertThat(files.toList()).isEmpty();
    }
  }
}
