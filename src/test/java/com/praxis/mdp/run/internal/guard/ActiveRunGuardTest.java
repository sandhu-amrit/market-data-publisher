package com.praxis.mdp.run.internal.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.praxis.mdp.pipeline.RunParameters;
import com.praxis.mdp.run.internal.guard.RunExceptions.RunAlreadyActiveException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;

class ActiveRunGuardTest {

  @Test
  void rejectsASecondRunOfTheSameJobAndDate() {
    LocalDate date = LocalDate.parse("2026-11-11");
    JobExecution running = mock(JobExecution.class);
    when(running.getJobParameters()).thenReturn(RunParameters.of(date, 1, "bob", "API"));
    JobExplorer explorer = mock(JobExplorer.class);
    when(explorer.findRunningJobExecutions("fxFuturesEodJob")).thenReturn(Set.of(running));

    ActiveRunGuard guard = new ActiveRunGuard(explorer);

    assertThatThrownBy(() -> guard.assertNoActiveRun("fxFuturesEodJob", date))
        .isInstanceOf(RunAlreadyActiveException.class);
  }

  @Test
  void sequenceStartsAtOne() {
    JobExplorer explorer = mock(JobExplorer.class);
    when(explorer.getJobInstances("fxFuturesEodJob", 0, 200)).thenReturn(List.of());
    ActiveRunGuard guard = new ActiveRunGuard(explorer);
    assertThat(guard.nextSequence("fxFuturesEodJob", LocalDate.parse("2026-11-11"))).isEqualTo(1);
  }
}
