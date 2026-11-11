package com.praxis.mdp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.praxis.mdp.api.RunAlreadyActiveException;
import com.praxis.mdp.api.RunService;
import com.praxis.mdp.batch.RunParameters;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;

class RunServiceTest {

    @Test
    void rejectsSecondRunOfSameJobAndDateWhileOneIsRunning() throws Exception {
        LocalDate date = LocalDate.parse("2026-11-11");
        JobExecution running = mock(JobExecution.class);
        when(running.getJobParameters()).thenReturn(RunParameters.of(date, 1, "bob", "API"));
        JobExplorer explorer = mock(JobExplorer.class);
        when(explorer.findRunningJobExecutions("fxFuturesEodJob")).thenReturn(Set.of(running));

        RunService service = new RunService(Map.of("fxFuturesEodJob", mock(Job.class)), mock(JobRepository.class), explorer);

        assertThatThrownBy(() -> service.launch("fxFuturesEodJob", date, "alice", "API"))
                .isInstanceOf(RunAlreadyActiveException.class);
    }
}
