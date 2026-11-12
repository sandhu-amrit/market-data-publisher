package com.praxis.mdp.publishing.internal.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.praxis.mdp.infrastructure.config.MdpProperties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;

class KafkaProducerConfigTest {

  @Test
  void producerIsIdempotentAndAcksAll() {
    MdpProperties.Kafka kafka = new MdpProperties().getDestinations().getKafka();
    kafka.setBootstrapServers("localhost:9092");
    assertThat(KafkaProducerConfig.producerConfig(kafka))
        .containsEntry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true)
        .containsEntry(ProducerConfig.ACKS_CONFIG, "all");
  }
}
