package com.praxis.mdp.publishing.internal.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.praxis.mdp.infrastructure.config.Connections.KafkaConnection;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;

class KafkaProducerConfigTest {

  @Test
  void producerIsIdempotentAndAcksAll() {
    KafkaConnection kafka = KafkaConnection.open("localhost:9092", false);
    assertThat(KafkaProducerConfig.producerConfig(kafka))
        .containsEntry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true)
        .containsEntry(ProducerConfig.ACKS_CONFIG, "all");
  }
}
