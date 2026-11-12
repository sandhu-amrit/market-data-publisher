package com.praxis.mdp.domain.policy;

/** What to do when a source row has no end-of-day price. Configurable per job. */
public enum MissingPricePolicy {
  /** Skip the row, log an alert, and keep going. Does not consume the invalid-record skip limit. */
  SKIP_AND_ALERT,
  /** Fail the extract step (and therefore the job) on the first missing price. */
  FAIL_RUN
}
