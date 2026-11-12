package com.praxis.mdp.pipeline.internal.validate;

/** A record that must not be published. Skipped up to the job's skip limit. */
public class InvalidRecordException extends RuntimeException {

  public InvalidRecordException(String message) {
    super(message);
  }
}
