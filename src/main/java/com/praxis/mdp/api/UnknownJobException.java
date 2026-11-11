package com.praxis.mdp.api;

public class UnknownJobException extends RuntimeException {
    public UnknownJobException(String jobName) {
        super("Unknown job '" + jobName + "'");
    }
}
