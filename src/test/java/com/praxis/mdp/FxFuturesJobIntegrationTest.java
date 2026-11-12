package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.praxis.mdp.pipeline.RunParameters;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class FxFuturesJobIntegrationTest {

  private static final Path OUTPUT = TestDirs.newTempDir("mdp-job-out");
  private static final Path WORK = TestDirs.newTempDir("mdp-job-work");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("mdp.output-dir", OUTPUT::toString);
    registry.add("mdp.work-dir", WORK::toString);
  }

  @Autowired TaskExecutorJobLauncher launcher;

  @Autowired JobExplorer explorer;

  @Autowired
  @Qualifier("fxFuturesEodBatchJob")
  Job job;

  @Autowired JobRepository jobRepository;

  @BeforeEach
  void clean() throws Exception {
    JobRepositoryTestUtils utils = new JobRepositoryTestUtils(jobRepository);
    utils.removeJobExecutions();
    if (Files.exists(OUTPUT)) {
      try (var files = Files.list(OUTPUT)) {
        for (Path path : files.toList()) {
          Files.deleteIfExists(path);
        }
      }
    }
  }

  @Test
  void publishesValidRecordsAndSkipsInvalidOnes() throws Exception {
    JobExecution execution =
        finished(
            launcher.run(job, RunParameters.of(LocalDate.parse("2026-11-11"), 1, "test", "TEST")));

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    StepExecution extract = step(execution, "extract");
    assertThat(extract.getReadCount()).isEqualTo(7);
    assertThat(extract.getReadSkipCount()).isEqualTo(1);
    assertThat(extract.getProcessSkipCount()).isEqualTo(2);
    assertThat(extract.getWriteCount()).isEqualTo(5);
    StepExecution publish = step(execution, "publish-file");
    assertThat(publish.getReadCount()).isEqualTo(5);
    assertThat(publish.getWriteCount()).isEqualTo(5);

    Path out = OUTPUT.resolve("FX_FUTURE_EOD_2026-11-11.csv");
    List<String> lines = Files.readAllLines(out);
    assertThat(lines).hasSize(6);
    assertThat(lines.get(1)).isEqualTo("2026-11-11,FXF-0001,FX_FUTURE,6EZ6,USD,2026-12-14,1.0842");
    assertThat(lines)
        .noneMatch(
            line ->
                line.contains("FXF-0005")
                    || line.contains("FXF-0006")
                    || line.contains("FXF-0007"));
    assertThat(OUTPUT.resolve("FX_FUTURE_EOD_2026-11-11.csv.tmp")).doesNotExist();
  }

  @Test
  void missingInputFileFailsTheRunAndLeavesNoOutput() throws Exception {
    JobExecution execution =
        finished(
            launcher.run(job, RunParameters.of(LocalDate.parse("2020-01-01"), 1, "test", "TEST")));
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
    try (var files = Files.list(OUTPUT)) {
      assertThat(files.toList()).isEmpty();
    }
  }

  private JobExecution finished(JobExecution started) {
    await()
        .atMost(Duration.ofSeconds(20))
        .until(
            () -> {
              JobExecution current = explorer.getJobExecution(started.getId());
              return current != null && !current.isRunning();
            });
    return explorer.getJobExecution(started.getId());
  }

  private static StepExecution step(JobExecution execution, String name) {
    return execution.getStepExecutions().stream()
        .filter(step -> step.getStepName().equals(name))
        .findFirst()
        .orElseThrow();
  }
}
