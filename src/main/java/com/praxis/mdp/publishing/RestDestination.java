package com.praxis.mdp.publishing;

import com.praxis.mdp.infrastructure.config.Connections.RestConnection;

/** REST publication bound to one {@link RestConnection} profile. The path is fixed in Java. */
public final class RestDestination {

  private RestDestination() {}

  public static Builder to(RestConnection connection) {
    return new Builder(connection);
  }

  public static final class Builder implements Publication {
    private final RestConnection connection;
    private String path = "/api/eod";
    private Format format = Format.JSON;

    private Builder(RestConnection connection) {
      if (connection == null) {
        throw new IllegalArgumentException("REST connection is required");
      }
      this.connection = connection;
    }

    public Builder path(String path) {
      this.path = path;
      return this;
    }

    public Builder format(Format format) {
      this.format = format;
      return this;
    }

    public RestConnection connection() {
      return connection;
    }

    public String path() {
      return path;
    }

    @Override
    public String stepName() {
      return "rest";
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
      return clients
          .rest()
          .sink(connection, path)
          .open(request.jobName(), request.type(), request.runId());
    }
  }
}
