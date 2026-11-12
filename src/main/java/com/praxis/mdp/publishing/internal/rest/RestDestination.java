package com.praxis.mdp.publishing.internal.rest;

import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.publishing.Destination;
import com.praxis.mdp.publishing.MarketDataSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "mdp.destinations.rest", name = "enabled", havingValue = "true")
public class RestDestination implements Destination {

  private final RestMarketDataSink sink;

  public RestDestination(MdpProperties props) {
    this.sink = new RestMarketDataSink(props);
  }

  @Override
  public String name() {
    return "rest";
  }

  @Override
  public int order() {
    return 40;
  }

  @Override
  public MarketDataSink sink() {
    return sink;
  }
}
