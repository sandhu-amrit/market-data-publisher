package com.praxis.mdp.publishing.internal.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.publishing.Destination;
import com.praxis.mdp.publishing.MarketDataSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "mdp.destinations.kafka", name = "enabled", havingValue = "true")
public class KafkaDestination implements Destination {

  private final KafkaMarketDataSink sink;

  public KafkaDestination(MdpProperties props, ObjectMapper json) {
    var kafka = props.getDestinations().getKafka();
    var factory =
        new DefaultKafkaProducerFactory<String, String>(KafkaProducerConfig.producerConfig(kafka));
    this.sink = new KafkaMarketDataSink(kafka.getTopic(), new KafkaTemplate<>(factory), json);
  }

  @Override
  public String name() {
    return "kafka";
  }

  @Override
  public int order() {
    return 30;
  }

  @Override
  public MarketDataSink sink() {
    return sink;
  }
}
