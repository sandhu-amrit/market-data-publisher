package com.praxis.mdp.batch;

/** Thrown by the processor for a record that must not be published. Configured as skippable. */
public class InvalidRecordException extends RuntimeException {

    public InvalidRecordException(String message) {
        super(message);
    }
}
