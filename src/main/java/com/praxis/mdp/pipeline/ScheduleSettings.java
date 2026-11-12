package com.praxis.mdp.pipeline;

/** Cron from {@code mdp.jobs.<name>.schedule}. Null cron means on-demand. */
public record ScheduleSettings(String cron, boolean enabled) {

  public boolean scheduled() {
    return enabled && cron != null && !cron.isBlank();
  }
}
