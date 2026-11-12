package com.praxis.mdp.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Environment values and batch tuning. Job shape (source, destinations, policies) is Java. A change
 * needs a restart; there is no refresh scope and the run API cannot override this.
 */
@Validated
@ConfigurationProperties(prefix = "mdp")
public class MdpProperties {

  @NotNull private Path outputDir = Path.of("output");

  @NotNull private Path workDir = Path.of("work");

  /** IANA zone for scheduler business dates and cron. Empty uses the JVM zone. */
  private String scheduleZone = "";

  private final Security security = new Security();
  private final Batch batch = new Batch();
  private final Cli cli = new Cli();
  private final Map<String, JobConfig> jobs = new LinkedHashMap<>();

  public Path getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(Path outputDir) {
    this.outputDir = outputDir;
  }

  public Path getWorkDir() {
    return workDir;
  }

  public void setWorkDir(Path workDir) {
    this.workDir = workDir;
  }

  public String getScheduleZone() {
    return scheduleZone;
  }

  public void setScheduleZone(String scheduleZone) {
    this.scheduleZone = scheduleZone;
  }

  public Security getSecurity() {
    return security;
  }

  public Batch getBatch() {
    return batch;
  }

  public Cli getCli() {
    return cli;
  }

  public Map<String, JobConfig> getJobs() {
    return jobs;
  }

  public enum SecurityMode {
    PERMIT_ALL,
    JWT
  }

  public static class Security {
    private SecurityMode mode = SecurityMode.PERMIT_ALL;

    public SecurityMode getMode() {
      return mode;
    }

    public void setMode(SecurityMode mode) {
      this.mode = mode;
    }
  }

  public static class Batch {
    @jakarta.validation.Valid private final Defaults defaults = new Defaults();

    public Defaults getDefaults() {
      return defaults;
    }
  }

  public static class Defaults {
    @Min(1)
    private int chunkSize = 1000;

    @Min(1)
    private int threads = 1;

    @Min(0)
    private int skipLimit = 100;

    @Min(0)
    private int retryLimit = 0;

    @Min(1)
    private int fetchSize = 1000;

    public int getChunkSize() {
      return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
      this.chunkSize = chunkSize;
    }

    public int getThreads() {
      return threads;
    }

    public void setThreads(int threads) {
      this.threads = threads;
    }

    public int getSkipLimit() {
      return skipLimit;
    }

    public void setSkipLimit(int skipLimit) {
      this.skipLimit = skipLimit;
    }

    public int getRetryLimit() {
      return retryLimit;
    }

    public void setRetryLimit(int retryLimit) {
      this.retryLimit = retryLimit;
    }

    public int getFetchSize() {
      return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
      this.fetchSize = fetchSize;
    }
  }

  /** Per-job environment values. Connection and destination details are not accepted here. */
  public static class JobConfig {
    /** Spring 6-field cron, {@code none}, or absent for on-demand. */
    private String schedule;

    /** When false the cron is not registered. The API can still launch the job. */
    private Boolean enabled;

    @jakarta.validation.Valid private final Tuning batch = new Tuning();

    public String getSchedule() {
      return schedule;
    }

    public void setSchedule(String schedule) {
      this.schedule = schedule;
    }

    public Boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }

    public Tuning getBatch() {
      return batch;
    }
  }

  /** Null fields inherit {@link Defaults}. */
  public static class Tuning {
    @Min(1)
    private Integer chunkSize;

    @Min(1)
    private Integer threads;

    @Min(0)
    private Integer skipLimit;

    @Min(0)
    private Integer retryLimit;

    @Min(1)
    private Integer fetchSize;

    public Integer getChunkSize() {
      return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
      this.chunkSize = chunkSize;
    }

    public Integer getThreads() {
      return threads;
    }

    public void setThreads(Integer threads) {
      this.threads = threads;
    }

    public Integer getSkipLimit() {
      return skipLimit;
    }

    public void setSkipLimit(Integer skipLimit) {
      this.skipLimit = skipLimit;
    }

    public Integer getRetryLimit() {
      return retryLimit;
    }

    public void setRetryLimit(Integer retryLimit) {
      this.retryLimit = retryLimit;
    }

    public Integer getFetchSize() {
      return fetchSize;
    }

    public void setFetchSize(Integer fetchSize) {
      this.fetchSize = fetchSize;
    }
  }

  public static class Cli {
    private String job = "";
    private String businessDate = "";
    private String triggeredBy = "";

    public String getJob() {
      return job;
    }

    public void setJob(String job) {
      this.job = job;
    }

    public String getBusinessDate() {
      return businessDate;
    }

    public void setBusinessDate(String businessDate) {
      this.businessDate = businessDate;
    }

    public String getTriggeredBy() {
      return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
      this.triggeredBy = triggeredBy;
    }
  }
}
