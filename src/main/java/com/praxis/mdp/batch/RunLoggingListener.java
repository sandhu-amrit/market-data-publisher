package com.praxis.mdp.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

/** Puts runId, jobName and businessDate into the logging MDC for the duration of a run, and logs a summary. */
@Component
public class RunLoggingListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(RunLoggingListener.class);

    @Override
    public void beforeJob(JobExecution je) {
        MDC.put("runId", String.valueOf(je.getId()));
        MDC.put("jobName", je.getJobInstance().getJobName());
        MDC.put("businessDate", je.getJobParameters().getString(RunParameters.BUSINESS_DATE));
        log.info("Run started (runSequence={}, triggeredBy={}, triggerSource={})",
                je.getJobParameters().getLong(RunParameters.RUN_SEQUENCE),
                je.getJobParameters().getString(RunParameters.TRIGGERED_BY),
                je.getJobParameters().getString(RunParameters.TRIGGER_SOURCE));
    }

    @Override
    public void afterJob(JobExecution je) {
        try {
            for (StepExecution se : je.getStepExecutions()) {
                log.info("Step {}: {} read={} written={} skipped={} (read={}, process={}, write={})",
                        se.getStepName(), se.getStatus(), se.getReadCount(), se.getWriteCount(), se.getSkipCount(),
                        se.getReadSkipCount(), se.getProcessSkipCount(), se.getWriteSkipCount());
            }
            log.info("Run finished with status {}", je.getStatus());
            je.getAllFailureExceptions().forEach(t -> log.error("Run failure: {}", t.toString()));
        } finally {
            MDC.clear();
        }
    }
}
