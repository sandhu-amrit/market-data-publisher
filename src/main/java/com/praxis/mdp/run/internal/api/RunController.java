package com.praxis.mdp.run.internal.api;

import com.praxis.mdp.run.RunService;
import com.praxis.mdp.run.RunStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.batch.core.BatchStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/runs")
public class RunController {

  static final String TRIGGER_SOURCE = "API";

  private final RunService runs;

  public RunController(RunService runs) {
    this.runs = runs;
  }

  @PostMapping
  public ResponseEntity<RunStatus> start(@Valid @RequestBody RunRequest request) {
    RunStatus status =
        runs.launch(request.jobName(), request.businessDate(), triggeredBy(), TRIGGER_SOURCE);
    return ResponseEntity.accepted()
        .location(URI.create("/api/v1/runs/" + status.executionId()))
        .body(status);
  }

  @GetMapping("/{id}")
  public RunStatus get(@PathVariable long id) {
    return runs.find(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No run with id " + id));
  }

  @GetMapping
  public java.util.List<RunStatus> list(
      @RequestParam(required = false) String job,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String status) {
    return runs.list(blankToNull(job), date, parseStatus(status));
  }

  @PostMapping("/{id}/restart")
  public ResponseEntity<RunStatus> restart(@PathVariable long id) {
    return ResponseEntity.accepted().body(runs.restart(id));
  }

  @PostMapping("/{id}/stop")
  public ResponseEntity<RunStatus> stop(@PathVariable long id) {
    return ResponseEntity.accepted().body(runs.stop(id));
  }

  private static String triggeredBy() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
      return "anonymous";
    }
    return auth.getName();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static BatchStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    try {
      return BatchStatus.valueOf(status.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status '" + status + "'");
    }
  }
}
