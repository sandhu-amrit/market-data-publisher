package com.praxis.mdp.pipeline;

import com.praxis.mdp.domain.RunId;
import java.time.LocalDate;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

/** Identifying parameters: businessDate + runSequence. Audit-only: triggeredBy, triggerSource. */
public final class RunParameters {

  public static final String BUSINESS_DATE = "businessDate";
  public static final String RUN_SEQUENCE = "runSequence";
  public static final String TRIGGERED_BY = "triggeredBy";
  public static final String TRIGGER_SOURCE = "triggerSource";

  private RunParameters() {}

  public static JobParameters of(
      LocalDate businessDate, long runSequence, String triggeredBy, String triggerSource) {
    return new JobParametersBuilder()
        .addString(BUSINESS_DATE, businessDate.toString(), true)
        .addLong(RUN_SEQUENCE, runSequence, true)
        .addString(TRIGGERED_BY, triggeredBy == null ? "" : triggeredBy, false)
        .addString(TRIGGER_SOURCE, triggerSource == null ? "" : triggerSource, false)
        .toJobParameters();
  }

  public static LocalDate businessDate(JobParameters params) {
    return LocalDate.parse(params.getString(BUSINESS_DATE));
  }

  public static RunId runId(JobParameters params) {
    Long sequence = params.getLong(RUN_SEQUENCE);
    if (sequence == null) {
      throw new IllegalStateException("Job parameters are missing runSequence");
    }
    return RunId.of(businessDate(params), sequence);
  }
}
