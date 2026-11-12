package com.praxis.mdp.publishing.internal.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.praxis.mdp.domain.EodPrice;
import com.praxis.mdp.domain.Instrument;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.publishing.SinkWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

class KafkaMarketDataSinkTest {

  @Test
  @EnabledIf("dockerAvailable")
  void publishesKeyedByInstrumentId() throws Exception {
    try (KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.1"))) {
      kafka.start();
      var props =
          new com.praxis.mdp.infrastructure.config.MdpProperties().getDestinations().getKafka();
      props.setBootstrapServers(kafka.getBootstrapServers());
      props.setTopic("mdp.eod");
      ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
      var factory =
          new DefaultKafkaProducerFactory<String, String>(
              KafkaProducerConfig.producerConfig(props));
      KafkaTemplate<String, String> template = new KafkaTemplate<>(factory);
      KafkaMarketDataSink sink = new KafkaMarketDataSink("mdp.eod", template, json);
      MarketDataRecord record =
          new MarketDataRecord(
              new Instrument(
                  "FXF-1", InstrumentType.FX_FUTURE, "6EZ6", "USD", LocalDate.parse("2026-12-14")),
              new EodPrice(LocalDate.parse("2026-11-11"), new BigDecimal("1.2")));
      SinkWriter writer =
          sink.open(
              "fxFuturesEodJob",
              InstrumentType.FX_FUTURE,
              RunId.of(LocalDate.parse("2026-11-11"), 1));
      writer.open(new ExecutionContext());
      writer.write(new Chunk<>(List.of(record)));
      writer.finish(true);

      try (KafkaConsumer<String, String> consumer =
          new KafkaConsumer<>(
              Map.of(
                  ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                  kafka.getBootstrapServers(),
                  ConsumerConfig.GROUP_ID_CONFIG,
                  "mdp-test",
                  ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                  "earliest",
                  ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                  StringDeserializer.class,
                  ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                  StringDeserializer.class))) {
        consumer.subscribe(List.of("mdp.eod"));
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 20000;
        while (records.isEmpty() && System.currentTimeMillis() < deadline) {
          consumer.poll(Duration.ofMillis(500)).forEach(records::add);
        }
        assertThat(records).hasSize(1);
        assertThat(records.get(0).key()).isEqualTo("FXF-1");
        assertThat(records.get(0).value()).contains("FXF-1");
      }
      factory.destroy();
    }
  }

  static boolean dockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable ignored) {
      return false;
    }
  }
}
