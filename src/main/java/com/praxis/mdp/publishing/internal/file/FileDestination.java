package com.praxis.mdp.publishing.internal.file;

import com.praxis.mdp.publishing.Destination;
import com.praxis.mdp.publishing.MarketDataSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "mdp.destinations.file",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class FileDestination implements Destination {

  private final FileMarketDataSink sink;

  public FileDestination(FileMarketDataSink sink) {
    this.sink = sink;
  }

  @Override
  public String name() {
    return "file";
  }

  @Override
  public int order() {
    return 10;
  }

  @Override
  public MarketDataSink sink() {
    return sink;
  }
}
