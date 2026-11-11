package com.praxis.mdp.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import com.praxis.mdp.batch.RunParameters;

/** Read model returned by the run API. */
public record RunStatus(
        long executionId,
        String jobName,
        LocalDate businessDate,
        Long runSequence,
        String status,
        String exitCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String triggeredBy,
        String triggerSource,
        List<StepSummary> steps) {

    public record StepSummary(
            String name, String status, long readCount, long writeCount, long filterCount,
            long readSkipCount, long processSkipCount, long writeSkipCount, long commitCount) {
    }

    public static RunStatus from(JobExecution je) {
        var p = je.getJobParameters();
        List<StepSummary> steps = je.getStepExecutions().stream()
                .sorted(java.util.Comparator.comparing(StepExecution::getId))
                .map(se -> new StepSummary(se.getStepName(), se.getStatus().name(), se.getReadCount(),
                        se.getWriteCount(), se.getFilterCount(), se.getReadSkipCount(), se.getProcessSkipCount(),
                        se.getWriteSkipCount(), se.getCommitCount()))
                .toList();
        return new RunStatus(je.getId(), je.getJobInstance().getJobName(), RunParameters.businessDate(p),
                p.getLong(RunParameters.RUN_SEQUENCE), je.getStatus().name(), je.getExitStatus().getExitCode(),
                je.getStartTime(), je.getEndTime(), p.getString(RunParameters.TRIGGERED_BY),
                p.getString(RunParameters.TRIGGER_SOURCE), steps);
    }
}
