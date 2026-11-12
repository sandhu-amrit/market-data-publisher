package com.praxis.mdp.publishing;

import com.praxis.mdp.infrastructure.config.Connections.KafkaConnection;
import com.praxis.mdp.publishing.internal.kafka.KafkaMarketDataSink;

/** Kafka publication bound to one {@link KafkaConnection} profile. The topic is fixed in Java. */
public final class KafkaDestination {

  private KafkaDestination() {}

  public static Builder to(KafkaConnection connection) {
    return new Builder(connection);
  }

  public static final class Builder implements Publication {
    private final KafkaConnection connection;
    private String topic = "mdp.eod";
    private Format format = Format.JSON;

    private Builder(KafkaConnection connection) {
      if (connection == null) {
        throw new IllegalArgumentException("Kafka connection is required");
      }
      this.connection = connection;
    }

    public Builder topic(String topic) {
      this.topic = topic;
      return this;
    }

    public Builder format(Format format) {
      this.format = format;
      return this;
    }

    public KafkaConnection connection() {
      return connection;
    }

    public String topic() {
      return topic;
    }

    @Override
    public String stepName() {
      return "kafka";
    }

    @Override
    public Format format() {
      return format;
    }

    @Override
    public boolean active() {
      return connection.enabled();
    }

    @Override
    public SinkWriter open(PublishRequest request, PublishingClients clients) {
      var producers = clients.kafka();
      return new KafkaMarketDataSink(topic, producers.template(connection), producers.json())
          .open(request.jobName(), request.type(), request.runId());
    }
  }
}
