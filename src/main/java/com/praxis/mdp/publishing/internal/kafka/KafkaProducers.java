package com.praxis.mdp.publishing.internal.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.praxis.mdp.infrastructure.config.Connections.KafkaConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** One idempotent producer per Kafka profile, created on first send. */
@Component
public class KafkaProducers {

  private final ObjectMapper json;
  private final ConcurrentMap<KafkaConnection, KafkaTemplate<String, String>> cache =
      new ConcurrentHashMap<>();

  public KafkaProducers(ObjectMapper json) {
    this.json = json;
  }

  public KafkaTemplate<String, String> template(KafkaConnection connection) {
    if (!connection.enabled()) {
      throw new IllegalStateException(
          "Kafka profile " + connection.bootstrapServers() + " is disabled");
    }
    return cache.computeIfAbsent(connection, this::create);
  }

  public ObjectMapper json() {
    return json;
  }

  public int size() {
    return cache.size();
  }

  private KafkaTemplate<String, String> create(KafkaConnection connection) {
    DefaultKafkaProducerFactory<String, String> factory =
        new DefaultKafkaProducerFactory<>(KafkaProducerConfig.producerConfig(connection));
    return new KafkaTemplate<>(factory);
  }
}
