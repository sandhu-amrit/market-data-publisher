package com.praxis.mdp.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Named connection profiles. The Java field ({@code vendorA}, {@code main}, {@code riskApi}) is the
 * profile. A missing field is a compile error at the job that calls it. Secrets are {@code ${ENV}}
 * placeholders only.
 */
@Validated
@ConfigurationProperties(prefix = "mdp.connections")
public record Connections(
    @Valid @NotNull SftpConnections sftp,
    @Valid @NotNull KafkaConnections kafka,
    @Valid @NotNull RestConnections rest,
    @Valid @NotNull SourceConnections source) {

  public record SftpConnections(
      @Valid @NotNull SftpConnection vendorA, @Valid @NotNull SftpConnection internal) {}

  public record KafkaConnections(@Valid @NotNull KafkaConnection main) {}

  public record RestConnections(@Valid @NotNull RestConnection riskApi) {}

  public record SourceConnections(@Valid @NotNull SourceConnection assetControl) {}

  public record SftpConnection(
      @NotBlank String host,
      @Min(1) @Max(65535) @DefaultValue("22") int port,
      @NotBlank String username,
      @DefaultValue("") String password,
      @DefaultValue("") String privateKey,
      @DefaultValue("") String knownHosts,
      @DefaultValue("false") boolean allowUnknownKeys,
      @DefaultValue("15s") Duration timeout,
      @DefaultValue("false") boolean enabled) {

    public static SftpConnection open(
        String host,
        int port,
        String username,
        String password,
        boolean allowUnknownKeys,
        boolean enabled) {
      return new SftpConnection(
          host,
          port,
          username,
          password,
          "",
          "",
          allowUnknownKeys,
          Duration.ofSeconds(15),
          enabled);
    }
  }

  public record KafkaConnection(
      @NotBlank String bootstrapServers,
      @DefaultValue("PLAINTEXT") String securityProtocol,
      @DefaultValue("") String truststoreLocation,
      @DefaultValue("") String truststorePassword,
      @DefaultValue("market-data-publisher") String clientId,
      @DefaultValue("false") boolean enabled) {

    public static KafkaConnection open(String bootstrapServers, boolean enabled) {
      return new KafkaConnection(
          bootstrapServers, "PLAINTEXT", "", "", "market-data-publisher", enabled);
    }
  }

  public record RestConnection(
      @NotBlank String baseUrl,
      @DefaultValue("none") String auth,
      @DefaultValue("") String clientId,
      @DefaultValue("") String clientSecret,
      @DefaultValue("") String tokenUri,
      @DefaultValue("2s") Duration connectTimeout,
      @DefaultValue("10s") Duration readTimeout,
      @DefaultValue("3") int retryAttempts,
      @DefaultValue("200ms") Duration retryWait,
      @DefaultValue("50") float circuitBreakerFailureRate,
      @DefaultValue("20") int circuitBreakerSlidingWindow,
      @DefaultValue("10") int circuitBreakerMinimumCalls,
      @DefaultValue("false") boolean enabled) {}

  public record SourceConnection(
      @NotNull Path inputDir, @DefaultValue("{type}_{businessDate}.csv") String filePattern) {}
}
