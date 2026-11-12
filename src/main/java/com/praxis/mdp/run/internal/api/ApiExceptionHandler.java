package com.praxis.mdp.run.internal.api;

import com.praxis.mdp.run.internal.guard.RunExceptions.InvalidRunStateException;
import com.praxis.mdp.run.internal.guard.RunExceptions.RunAlreadyActiveException;
import com.praxis.mdp.run.internal.guard.RunExceptions.UnknownJobException;
import com.praxis.mdp.run.internal.guard.RunExceptions.UnknownRunException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps run-control errors to RFC 7807 problem details. Bean validation is handled by Spring. */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(UnknownJobException.class)
  ProblemDetail unknownJob(UnknownJobException e) {
    return problem(HttpStatus.NOT_FOUND, "unknown-job", "Unknown job", e.getMessage());
  }

  @ExceptionHandler(UnknownRunException.class)
  ProblemDetail unknownRun(UnknownRunException e) {
    return problem(HttpStatus.NOT_FOUND, "unknown-run", "Unknown run", e.getMessage());
  }

  @ExceptionHandler(RunAlreadyActiveException.class)
  ProblemDetail alreadyRunning(RunAlreadyActiveException e) {
    return problem(HttpStatus.CONFLICT, "run-already-active", "Run already active", e.getMessage());
  }

  @ExceptionHandler(InvalidRunStateException.class)
  ProblemDetail invalidState(InvalidRunStateException e) {
    return problem(HttpStatus.CONFLICT, "invalid-run-state", "Invalid run state", e.getMessage());
  }

  private static ProblemDetail problem(
      HttpStatus status, String type, String title, String detail) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
    body.setTitle(title);
    body.setType(URI.create("https://mdp.praxis.com/problems/" + type));
    return body;
  }
}
