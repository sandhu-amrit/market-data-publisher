package com.praxis.mdp.infrastructure.observability;

import org.slf4j.MDC;

/** MDC keys included in structured logs for the duration of a run. */
public final class RunMdc {

  public static final String JOB_NAME = "jobName";
  public static final String BUSINESS_DATE = "businessDate";
  public static final String EXECUTION_ID = "executionId";
  public static final String RUN_SEQUENCE = "runSequence";
  public static final String TRIGGERED_BY = "triggeredBy";

  private RunMdc() {}

  public static void put(
      String jobName,
      String businessDate,
      String executionId,
      String runSequence,
      String triggeredBy) {
    MDC.put(JOB_NAME, nullToEmpty(jobName));
    MDC.put(BUSINESS_DATE, nullToEmpty(businessDate));
    MDC.put(EXECUTION_ID, nullToEmpty(executionId));
    MDC.put(RUN_SEQUENCE, nullToEmpty(runSequence));
    MDC.put(TRIGGERED_BY, nullToEmpty(triggeredBy));
  }

  public static String jobName() {
    String value = MDC.get(JOB_NAME);
    return value == null || value.isBlank() ? "unknown" : value;
  }

  public static void clear() {
    MDC.remove(JOB_NAME);
    MDC.remove(BUSINESS_DATE);
    MDC.remove(EXECUTION_ID);
    MDC.remove(RUN_SEQUENCE);
    MDC.remove(TRIGGERED_BY);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
