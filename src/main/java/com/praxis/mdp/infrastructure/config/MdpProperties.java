package com.praxis.mdp.infrastructure.config;

import com.praxis.mdp.domain.policy.FailurePolicy;
import com.praxis.mdp.domain.policy.MissingPricePolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed settings. Secrets are placeholders for environment variables (Vault-ready); nothing secret
 * is defaulted.
 */
@Validated
@ConfigurationProperties(prefix = "mdp")
public class MdpProperties {

  @NotNull private Path inputDir = Path.of("sample-data/input");

  @NotNull private Path outputDir = Path.of("output");

  @NotNull private Path workDir = Path.of("work");

  @Min(1)
  private int chunkSize = 1000;

  @Min(0)
  private int skipLimit = 100;

  /** IANA zone for scheduler business dates and cron. Empty uses the JVM zone. */
  private String scheduleZone = "";

  private final Security security = new Security();
  private final Destinations destinations = new Destinations();
  private final Cli cli = new Cli();
  private final Map<String, JobConfig> jobs = new LinkedHashMap<>();

  public Path getInputDir() {
    return inputDir;
  }

  public void setInputDir(Path inputDir) {
    this.inputDir = inputDir;
  }

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

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getSkipLimit() {
    return skipLimit;
  }

  public void setSkipLimit(int skipLimit) {
    this.skipLimit = skipLimit;
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

  public Destinations getDestinations() {
    return destinations;
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

  public static class JobConfig {
    private String cron;
    private Boolean scheduleEnabled;
    private FailurePolicy failurePolicy;
    private MissingPricePolicy missingPricePolicy;
    private List<String> destinations = new ArrayList<>();

    public String getCron() {
      return cron;
    }

    public void setCron(String cron) {
      this.cron = cron;
    }

    public Boolean getScheduleEnabled() {
      return scheduleEnabled;
    }

    public void setScheduleEnabled(Boolean scheduleEnabled) {
      this.scheduleEnabled = scheduleEnabled;
    }

    public FailurePolicy getFailurePolicy() {
      return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicy failurePolicy) {
      this.failurePolicy = failurePolicy;
    }

    public MissingPricePolicy getMissingPricePolicy() {
      return missingPricePolicy;
    }

    public void setMissingPricePolicy(MissingPricePolicy missingPricePolicy) {
      this.missingPricePolicy = missingPricePolicy;
    }

    public List<String> getDestinations() {
      return destinations;
    }

    public void setDestinations(List<String> destinations) {
      this.destinations = destinations;
    }
  }

  public static class Destinations {
    private final File file = new File();
    private final Sftp sftp = new Sftp();
    private final Kafka kafka = new Kafka();
    private final Rest rest = new Rest();

    public File getFile() {
      return file;
    }

    public Sftp getSftp() {
      return sftp;
    }

    public Kafka getKafka() {
      return kafka;
    }

    public Rest getRest() {
      return rest;
    }
  }

  public static class File {
    private boolean enabled = true;
    private Path directory;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Path getDirectory() {
      return directory;
    }

    public void setDirectory(Path directory) {
      this.directory = directory;
    }
  }

  public static class Sftp {
    private boolean enabled = false;
    private String host = "localhost";
    private int port = 22;
    private String username = "mdp";
    private String password = "";
    private String remoteDirectory = "upload";
    private boolean allowUnknownKeys = false;
    private Duration timeout = Duration.ofSeconds(15);

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getRemoteDirectory() {
      return remoteDirectory;
    }

    public void setRemoteDirectory(String remoteDirectory) {
      this.remoteDirectory = remoteDirectory;
    }

    public boolean isAllowUnknownKeys() {
      return allowUnknownKeys;
    }

    public void setAllowUnknownKeys(boolean allowUnknownKeys) {
      this.allowUnknownKeys = allowUnknownKeys;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }
  }

  public static class Kafka {
    private boolean enabled = false;
    private String bootstrapServers = "localhost:9092";
    private String topic = "mdp.eod";
    private String clientId = "market-data-publisher";
    private int lingerMs = 20;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBootstrapServers() {
      return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
      this.bootstrapServers = bootstrapServers;
    }

    public String getTopic() {
      return topic;
    }

    public void setTopic(String topic) {
      this.topic = topic;
    }

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }

    public int getLingerMs() {
      return lingerMs;
    }

    public void setLingerMs(int lingerMs) {
      this.lingerMs = lingerMs;
    }
  }

  public static class Rest {
    private boolean enabled = false;
    private URI baseUrl = URI.create("http://localhost:8081");
    private String path = "/api/eod";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(10);
    private int retryAttempts = 3;
    private Duration retryWait = Duration.ofMillis(200);
    private float circuitBreakerFailureRate = 50f;
    private int circuitBreakerSlidingWindow = 20;
    private int circuitBreakerMinimumCalls = 10;
    private String bearerToken = "";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public URI getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public Duration getConnectTimeout() {
      return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
      return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
      this.readTimeout = readTimeout;
    }

    public int getRetryAttempts() {
      return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
      this.retryAttempts = retryAttempts;
    }

    public Duration getRetryWait() {
      return retryWait;
    }

    public void setRetryWait(Duration retryWait) {
      this.retryWait = retryWait;
    }

    public float getCircuitBreakerFailureRate() {
      return circuitBreakerFailureRate;
    }

    public void setCircuitBreakerFailureRate(float circuitBreakerFailureRate) {
      this.circuitBreakerFailureRate = circuitBreakerFailureRate;
    }

    public int getCircuitBreakerSlidingWindow() {
      return circuitBreakerSlidingWindow;
    }

    public void setCircuitBreakerSlidingWindow(int circuitBreakerSlidingWindow) {
      this.circuitBreakerSlidingWindow = circuitBreakerSlidingWindow;
    }

    public int getCircuitBreakerMinimumCalls() {
      return circuitBreakerMinimumCalls;
    }

    public void setCircuitBreakerMinimumCalls(int circuitBreakerMinimumCalls) {
      this.circuitBreakerMinimumCalls = circuitBreakerMinimumCalls;
    }

    public String getBearerToken() {
      return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
      this.bearerToken = bearerToken;
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
