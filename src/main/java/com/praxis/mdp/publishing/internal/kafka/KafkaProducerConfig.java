package com.praxis.mdp.publishing.internal.kafka;

import com.praxis.mdp.infrastructure.config.Connections.KafkaConnection;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;

/** Idempotent producer settings. Records are keyed by instrument id so a replay last-write-wins. */
public final class KafkaProducerConfig {

  private KafkaProducerConfig() {}

  public static Map<String, Object> producerConfig(KafkaConnection kafka) {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers());
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    config.put(ProducerConfig.ACKS_CONFIG, "all");
    config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
    config.put(ProducerConfig.CLIENT_ID_CONFIG, kafka.clientId());
    config.put(ProducerConfig.LINGER_MS_CONFIG, 20);
    if (kafka.securityProtocol() != null && !kafka.securityProtocol().isBlank()) {
      config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafka.securityProtocol());
    }
    if (kafka.truststoreLocation() != null && !kafka.truststoreLocation().isBlank()) {
      config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafka.truststoreLocation());
      config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafka.truststorePassword());
    }
    return config;
  }
}
