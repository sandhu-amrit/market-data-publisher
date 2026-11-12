package com.praxis.mdp.publishing.internal.kafka;

import com.praxis.mdp.infrastructure.config.MdpProperties;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

/** Idempotent producer settings. Records are keyed by instrument id so a replay last-write-wins. */
public final class KafkaProducerConfig {

  private KafkaProducerConfig() {}

  public static Map<String, Object> producerConfig(MdpProperties.Kafka kafka) {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    config.put(ProducerConfig.ACKS_CONFIG, "all");
    config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
    config.put(ProducerConfig.CLIENT_ID_CONFIG, kafka.getClientId());
    config.put(ProducerConfig.LINGER_MS_CONFIG, kafka.getLingerMs());
    return config;
  }
}
