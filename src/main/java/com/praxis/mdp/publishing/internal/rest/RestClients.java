package com.praxis.mdp.publishing.internal.rest;

import com.praxis.mdp.infrastructure.config.Connections.RestConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/** One REST client per profile, created on first post. */
@Component
public class RestClients {

  private final ConcurrentMap<RestConnection, RestMarketDataSink> cache = new ConcurrentHashMap<>();

  public RestMarketDataSink sink(RestConnection connection, String path) {
    if (!connection.enabled()) {
      throw new IllegalStateException("REST profile " + connection.baseUrl() + " is disabled");
    }
    return cache.computeIfAbsent(connection, conn -> new RestMarketDataSink(conn, path));
  }

  public int size() {
    return cache.size();
  }
}
