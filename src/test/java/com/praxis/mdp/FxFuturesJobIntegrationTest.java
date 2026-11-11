package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;

import com.praxis.mdp.batch.RunParameters;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@SpringBatchTest
class FxFuturesJobIntegrationTest {

    private static final Path OUTPUT_DIR = TestDirs.newTempDir("mdp-job-out");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("mdp.input-dir", () -> "src/test/resources/input");
        r.add("mdp.output-dir", OUTPUT_DIR::toString);
    }

    @Autowired JobLauncherTestUtils jobLauncher;
    @Autowired JobRepositoryTestUtils jobRepositoryUtils;

    @BeforeEach
    void clean() throws IOException {
        jobRepositoryUtils.removeJobExecutions();
        try (var files = Files.list(OUTPUT_DIR)) {
            for (Path p : files.toList()) {
                Files.delete(p);
            }
        }
    }

    @Test
    void publishesValidRecordsAndSkipsInvalidOnes() throws Exception {
        JobExecution execution = jobLauncher.launchJob(RunParameters.of(LocalDate.parse("2026-11-11"), 1, "test", "TEST"));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getStepName()).isEqualTo("publishFile");
        assertThat(step.getReadCount()).isEqualTo(7);        // 8 data lines, 1 unparseable
        assertThat(step.getReadSkipCount()).isEqualTo(1);    // price "not-a-number"
        assertThat(step.getProcessSkipCount()).isEqualTo(2); // missing price, negative price
        assertThat(step.getWriteCount()).isEqualTo(5);

        Path out = OUTPUT_DIR.resolve("FX_FUTURE_EOD_2026-11-11.csv");
        List<String> lines = Files.readAllLines(out);
        assertThat(lines).hasSize(6); // header + 5 records
        assertThat(lines.get(0)).startsWith("businessDate,instrumentId");
        assertThat(lines.get(1)).isEqualTo("2026-11-11,FXF-0001,FX_FUTURE,6EZ6,USD,2026-12-14,1.0842");
        assertThat(lines).noneMatch(l -> l.contains("FXF-0005") || l.contains("FXF-0006") || l.contains("FXF-0007"));
        assertThat(OUTPUT_DIR.resolve("FX_FUTURE_EOD_2026-11-11.csv.tmp")).doesNotExist();
    }

    @Test
    void missingInputFileFailsTheRunAndLeavesNoOutput() throws Exception {
        JobExecution execution = jobLauncher.launchJob(RunParameters.of(LocalDate.parse("2020-01-01"), 1, "test", "TEST"));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        try (var files = Files.list(OUTPUT_DIR)) {
            assertThat(files.toList()).isEmpty();
        }
    }
}
