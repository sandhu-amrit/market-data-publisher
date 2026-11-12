package com.praxis.mdp.run.internal.guard;

public final class RunExceptions {

  private RunExceptions() {}

  public static class UnknownJobException extends RuntimeException {
    public UnknownJobException(String jobName) {
      super("Unknown job '" + jobName + "'");
    }
  }

  public static class UnknownRunException extends RuntimeException {
    public UnknownRunException(long executionId) {
      super("No run with id " + executionId);
    }
  }

  public static class RunAlreadyActiveException extends RuntimeException {
    public RunAlreadyActiveException(String message) {
      super(message);
    }
  }

  public static class InvalidRunStateException extends RuntimeException {
    public InvalidRunStateException(String message) {
      super(message);
    }
  }
}
