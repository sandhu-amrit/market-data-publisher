package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThat;

import com.praxis.mdp.pipeline.RunParameters;
import com.praxis.mdp.pipeline.internal.listener.StartupRecovery;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StartupRecoveryTest {

  @Autowired JobRepository jobRepository;

  @Autowired JobExplorer explorer;

  @Autowired StartupRecovery recovery;

  @Test
  void marksInterruptedExecutionsFailed() throws Exception {
    LocalDate date = LocalDate.parse("2019-05-05");
    JobExecution created =
        jobRepository.createJobExecution(
            "fxFuturesEodJob", RunParameters.of(date, 1, "crash", "API"));
    assertThat(created.isRunning()).isTrue();

    assertThat(recovery.recover()).isGreaterThanOrEqualTo(1);

    JobExecution recovered = explorer.getJobExecution(created.getId());
    assertThat(recovered.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(recovered.getExitStatus().getExitDescription()).contains("Marked FAILED on startup");
  }
}
