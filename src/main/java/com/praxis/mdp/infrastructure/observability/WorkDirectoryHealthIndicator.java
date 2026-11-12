package com.praxis.mdp.infrastructure.observability;

import com.praxis.mdp.infrastructure.config.MdpProperties;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WorkDirectoryHealthIndicator implements HealthIndicator {

  private final MdpProperties props;

  public WorkDirectoryHealthIndicator(MdpProperties props) {
    this.props = props;
  }

  @Override
  public Health health() {
    try {
      Files.createDirectories(props.getWorkDir());
      if (!Files.isWritable(props.getWorkDir())) {
        return Health.down().withDetail("path", props.getWorkDir().toString()).build();
      }
      return Health.up().withDetail("path", props.getWorkDir().toString()).build();
    } catch (IOException e) {
      return Health.down(e).withDetail("path", props.getWorkDir().toString()).build();
    }
  }
}
