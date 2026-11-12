package com.praxis.mdp.domain.policy;

/** What to do when one publish destination fails. Chosen in the Java job definition. */
public enum FailurePolicy {
  /** Stop the job at the first destination failure. Later destinations are not attempted. */
  FAIL_FAST,
  /** Publish the remaining destinations, then fail the job if any destination failed. */
  CONTINUE_OTHER_DESTINATIONS
}
