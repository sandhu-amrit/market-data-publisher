package com.praxis.mdp.pipeline;

import com.praxis.mdp.infrastructure.config.MdpProperties;
import org.springframework.stereotype.Component;

/** Merges {@code mdp.batch.defaults} with {@code mdp.jobs.<name>.batch} and reads the schedule. */
@Component
public class JobRuntime {

  private final MdpProperties props;

  public JobRuntime(MdpProperties props) {
    this.props = props;
  }

  public BatchSettings batch(String jobName) {
    MdpProperties.Defaults defaults = props.getBatch().getDefaults();
    MdpProperties.JobConfig cfg = props.getJobs().get(jobName);
    MdpProperties.Tuning override = cfg == null ? null : cfg.getBatch();
    try {
      return new BatchSettings(
          pick(override == null ? null : override.getChunkSize(), defaults.getChunkSize()),
          pick(override == null ? null : override.getThreads(), defaults.getThreads()),
          pick(override == null ? null : override.getSkipLimit(), defaults.getSkipLimit()),
          pick(override == null ? null : override.getRetryLimit(), defaults.getRetryLimit()),
          pick(override == null ? null : override.getFetchSize(), defaults.getFetchSize()));
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Job " + jobName + ": " + e.getMessage(), e);
    }
  }

  public ScheduleSettings schedule(String jobName) {
    MdpProperties.JobConfig cfg = props.getJobs().get(jobName);
    if (cfg == null) {
      return new ScheduleSettings(null, true);
    }
    String cron = cfg.getSchedule();
    if (cron != null && (cron.isBlank() || cron.equalsIgnoreCase("none"))) {
      cron = null;
    }
    boolean enabled = cfg.getEnabled() == null || cfg.getEnabled();
    return new ScheduleSettings(cron, enabled);
  }

  private static int pick(Integer override, int fallback) {
    return override == null ? fallback : override;
  }
}
