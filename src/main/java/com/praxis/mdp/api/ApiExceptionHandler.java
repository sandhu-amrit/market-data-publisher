package com.praxis.mdp.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain errors to RFC 7807 problem responses. Validation errors (400) are handled by Spring. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownJobException.class)
    ProblemDetail unknownJob(UnknownJobException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RunAlreadyActiveException.class)
    ProblemDetail alreadyRunning(RunAlreadyActiveException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
