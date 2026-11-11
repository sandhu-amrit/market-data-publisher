package com.praxis.mdp.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed, fail-fast application settings (prefix {@code mdp}). Only environment values live here;
 * what a job does is defined in Java (see {@code JobsConfig}).
 */
@Validated
@ConfigurationProperties(prefix = "mdp")
public record MdpProperties(
        @NotNull @DefaultValue("sample-data/input") Path inputDir,
        @NotNull @DefaultValue("output") Path outputDir,
        @Min(1) @DefaultValue("1000") int chunkSize,
        @Min(0) @DefaultValue("100") int skipLimit) {
}
