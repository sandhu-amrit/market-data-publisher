package com.praxis.mdp.api;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    static final String TRIGGER_SOURCE = "API";
    static final String DEFAULT_USER = "anonymous"; // replaced by the authenticated user once security is added

    private final RunService runs;

    public RunController(RunService runs) {
        this.runs = runs;
    }

    /** Starts a run; responds 202 immediately with the execution id. */
    @PostMapping
    public ResponseEntity<RunStatus> start(@Valid @RequestBody RunRequest request) {
        String user = request.triggeredBy() == null || request.triggeredBy().isBlank() ? DEFAULT_USER : request.triggeredBy();
        var execution = runs.launch(request.jobName(), request.businessDate(), user, TRIGGER_SOURCE);
        return ResponseEntity.accepted()
                .location(URI.create("/api/runs/" + execution.getId()))
                .body(RunStatus.from(execution));
    }

    @GetMapping("/{id}")
    public RunStatus get(@PathVariable long id) {
        return runs.find(id).map(RunStatus::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No run with id " + id));
    }
}
