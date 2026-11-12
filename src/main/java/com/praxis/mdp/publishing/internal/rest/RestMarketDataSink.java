package com.praxis.mdp.publishing.internal.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.publishing.MarketDataSink;
import com.praxis.mdp.publishing.SinkWriter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Posts each chunk as JSON. 5xx and I/O errors are retried; the circuit breaker opens on repeated
 * failures.
 */
public class RestMarketDataSink implements MarketDataSink {

  private final RestClient client;
  private final String path;
  private final Retry retry;
  private final CircuitBreaker circuitBreaker;

  public RestMarketDataSink(MdpProperties props) {
    MdpProperties.Rest rest = props.getDestinations().getRest();
    HttpClient http =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(rest.getConnectTimeout())
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(rest.getReadTimeout());
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    RestClient.Builder builder =
        RestClient.builder()
            .requestFactory(factory)
            .messageConverters(
                converters -> {
                  converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                  converters.add(new MappingJackson2HttpMessageConverter(mapper));
                })
            .baseUrl(rest.getBaseUrl().toString());
    if (rest.getBearerToken() != null && !rest.getBearerToken().isBlank()) {
      builder.defaultHeader("Authorization", "Bearer " + rest.getBearerToken());
    }
    this.client = builder.build();
    this.path = rest.getPath();
    this.retry =
        Retry.of(
            "mdpRestSink",
            RetryConfig.custom()
                .maxAttempts(Math.max(1, rest.getRetryAttempts()))
                .waitDuration(
                    rest.getRetryWait() == null ? Duration.ofMillis(200) : rest.getRetryWait())
                .retryExceptions(HttpServerErrorException.class, ResourceAccessException.class)
                .ignoreExceptions(CallNotPermittedException.class)
                .build());
    this.circuitBreaker =
        CircuitBreaker.of(
            "mdpRestSink",
            CircuitBreakerConfig.custom()
                .failureRateThreshold(rest.getCircuitBreakerFailureRate())
                .slidingWindowSize(Math.max(1, rest.getCircuitBreakerSlidingWindow()))
                .minimumNumberOfCalls(Math.max(1, rest.getCircuitBreakerMinimumCalls()))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build());
  }

  @Override
  public SinkWriter open(String jobName, InstrumentType type, RunId runId) {
    return new RestWriter(client, path, retry, circuitBreaker, jobName, runId);
  }

  record RestEodBatch(
      String jobName,
      String businessDate,
      long runSequence,
      int chunkIndex,
      List<MarketDataRecord> records) {}

  static final class RestWriter implements SinkWriter {

    private static final Logger log = LoggerFactory.getLogger(RestWriter.class);

    private final RestClient client;
    private final String path;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final String jobName;
    private final RunId runId;
    private int chunkIndex;
    private long written;

    RestWriter(
        RestClient client,
        String path,
        Retry retry,
        CircuitBreaker circuitBreaker,
        String jobName,
        RunId runId) {
      this.client = client;
      this.path = path;
      this.retry = retry;
      this.circuitBreaker = circuitBreaker;
      this.jobName = jobName;
      this.runId = runId;
    }

    @Override
    public void open(ExecutionContext executionContext) {}

    @Override
    public void write(Chunk<? extends MarketDataRecord> chunk) {
      int index = chunkIndex++;
      List<MarketDataRecord> records =
          chunk.getItems().stream().map(item -> (MarketDataRecord) item).toList();
      RestEodBatch body =
          new RestEodBatch(
              jobName, runId.businessDate().toString(), runId.runSequence(), index, records);
      String idempotencyKey =
          jobName + ":" + runId.businessDate() + ":" + runId.runSequence() + ":" + index;
      Supplier<Void> call =
          CircuitBreaker.decorateSupplier(
              circuitBreaker,
              () -> {
                client
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
                return null;
              });
      Retry.decorateSupplier(retry, call).get();
      written += records.size();
    }

    @Override
    public void update(ExecutionContext executionContext) {}

    @Override
    public void close() {}

    @Override
    public void finish(boolean success) {
      if (success) {
        log.info("Published {} records to REST {}", written, path);
      } else {
        log.warn(
            "REST publish for {} {} did not complete after {} records", jobName, runId, written);
      }
    }
  }
}
