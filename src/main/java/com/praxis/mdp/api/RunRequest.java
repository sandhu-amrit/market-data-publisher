package com.praxis.mdp.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Body of POST /api/runs. {@code businessDate} is ISO yyyy-MM-dd and may be today or any past date. */
public record RunRequest(@NotBlank String jobName, @NotNull LocalDate businessDate, String triggeredBy) {
}
