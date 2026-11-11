package com.praxis.mdp.api;

public class RunAlreadyActiveException extends RuntimeException {
    public RunAlreadyActiveException(String message) {
        super(message);
    }
}
