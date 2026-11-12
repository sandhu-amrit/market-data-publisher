package com.praxis.mdp.publishing.internal.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.publishing.MarketDataSink;
import com.praxis.mdp.publishing.SinkWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/** Publishes one JSON record per instrument, keyed by instrument id, on an idempotent producer. */
public class KafkaMarketDataSink implements MarketDataSink {

  private final String topic;
  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper json;

  public KafkaMarketDataSink(String topic, KafkaTemplate<String, String> kafka, ObjectMapper json) {
    this.topic = topic;
    this.kafka = kafka;
    this.json = json;
  }

  @Override
  public SinkWriter open(String jobName, InstrumentType type, RunId runId) {
    return new KafkaWriter(topic, kafka, json, jobName, type, runId);
  }

  record KafkaEodMessage(
      String jobName, String businessDate, long runSequence, MarketDataRecord record) {}

  static final class KafkaWriter implements SinkWriter {

    private static final Logger log = LoggerFactory.getLogger(KafkaWriter.class);

    private final String topic;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String jobName;
    private final InstrumentType type;
    private final RunId runId;
    private long written;

    KafkaWriter(
        String topic,
        KafkaTemplate<String, String> kafka,
        ObjectMapper json,
        String jobName,
        InstrumentType type,
        RunId runId) {
      this.topic = topic;
      this.kafka = kafka;
      this.json = json;
      this.jobName = jobName;
      this.type = type;
      this.runId = runId;
    }

    @Override
    public void open(ExecutionContext executionContext) {}

    @Override
    public void write(Chunk<? extends MarketDataRecord> chunk) {
      List<java.util.concurrent.Future<SendResult<String, String>>> futures = new ArrayList<>();
      for (MarketDataRecord record : chunk) {
        futures.add(kafka.send(record(record)));
        written++;
      }
      kafka.flush();
      for (var future : futures) {
        try {
          future.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Interrupted sending to " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
          throw new IllegalStateException("Failed to send to " + topic, e);
        }
      }
    }

    private ProducerRecord<String, String> record(MarketDataRecord record) {
      try {
        String payload =
            json.writeValueAsString(
                new KafkaEodMessage(
                    jobName, runId.businessDate().toString(), runId.runSequence(), record));
        ProducerRecord<String, String> produced =
            new ProducerRecord<>(topic, record.instrument().id(), payload);
        produced.headers().add(header("jobName", jobName));
        produced.headers().add(header("businessDate", runId.businessDate().toString()));
        produced.headers().add(header("runSequence", Long.toString(runId.runSequence())));
        produced.headers().add(header("instrumentType", type.name()));
        return produced;
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Cannot serialize " + record.instrument().id(), e);
      }
    }

    private static RecordHeader header(String key, String value) {
      return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void update(ExecutionContext executionContext) {}

    @Override
    public void close() {}

    @Override
    public void finish(boolean success) {
      if (success) {
        log.info("Published {} records to Kafka topic {}", written, topic);
      } else {
        log.warn(
            "Kafka publish for {} {} did not complete; {} records may already have been sent",
            jobName,
            runId,
            written);
      }
    }
  }
}
