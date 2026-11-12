package com.praxis.mdp.pipeline;

import java.time.LocalDate;

/**
 * Port the scheduling module calls to start a run. Implemented by the run module so scheduling does
 * not depend on it.
 */
public interface ScheduledRunLauncher {

  /**
   * Launches a scheduled run for {@code businessDate}. A duplicate active run is logged and
   * ignored.
   */
  void launchScheduled(String jobName, LocalDate businessDate);
}
