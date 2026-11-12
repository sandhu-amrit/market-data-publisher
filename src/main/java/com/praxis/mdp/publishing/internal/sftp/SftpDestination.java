package com.praxis.mdp.publishing.internal.sftp;

import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.publishing.Destination;
import com.praxis.mdp.publishing.MarketDataSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "mdp.destinations.sftp", name = "enabled", havingValue = "true")
public class SftpDestination implements Destination {

  private final SftpMarketDataSink sink;

  public SftpDestination(MdpProperties props) {
    this.sink = new SftpMarketDataSink(props);
  }

  @Override
  public String name() {
    return "sftp";
  }

  @Override
  public int order() {
    return 20;
  }

  @Override
  public MarketDataSink sink() {
    return sink;
  }
}
