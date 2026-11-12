package com.praxis.mdp.run.internal.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Body of {@code POST /api/v1/runs}. {@code triggeredBy} is taken from the authenticated principal,
 * not the body.
 */
public record RunRequest(@NotBlank String jobName, @NotNull LocalDate businessDate) {}
