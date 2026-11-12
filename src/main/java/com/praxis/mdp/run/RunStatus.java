package com.praxis.mdp.run;

import com.praxis.mdp.pipeline.RunParameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

/** Read model for one Spring Batch execution, including per-step counts. */
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
    List<StepStatus> steps) {

  public record StepStatus(
      String name,
      String status,
      String exitCode,
      long readCount,
      long writeCount,
      long filterCount,
      long readSkipCount,
      long processSkipCount,
      long writeSkipCount,
      long commitCount) {}

  public static RunStatus from(JobExecution execution) {
    var params = execution.getJobParameters();
    List<StepStatus> steps =
        execution.getStepExecutions().stream()
            .sorted(
                Comparator.comparing(StepExecution::getId, Comparator.nullsLast(Long::compareTo)))
            .map(
                step ->
                    new StepStatus(
                        step.getStepName(),
                        step.getStatus().name(),
                        step.getExitStatus().getExitCode(),
                        step.getReadCount(),
                        step.getWriteCount(),
                        step.getFilterCount(),
                        step.getReadSkipCount(),
                        step.getProcessSkipCount(),
                        step.getWriteSkipCount(),
                        step.getCommitCount()))
            .toList();
    return new RunStatus(
        execution.getId(),
        execution.getJobInstance().getJobName(),
        RunParameters.businessDate(params),
        params.getLong(RunParameters.RUN_SEQUENCE),
        execution.getStatus().name(),
        execution.getExitStatus().getExitCode(),
        execution.getStartTime(),
        execution.getEndTime(),
        params.getString(RunParameters.TRIGGERED_BY),
        params.getString(RunParameters.TRIGGER_SOURCE),
        steps);
  }
}
