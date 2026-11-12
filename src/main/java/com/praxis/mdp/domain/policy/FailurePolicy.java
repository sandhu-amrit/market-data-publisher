package com.praxis.mdp.domain.policy;

/** What to do when one publish destination fails. Configurable per job. */
public enum FailurePolicy {
  /** Stop the job at the first destination failure. Later destinations are not attempted. */
  FAIL_FAST,
  /** Publish the remaining destinations, then fail the job if any destination failed. */
  CONTINUE
}
