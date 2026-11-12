package com.praxis.mdp.publishing.internal;

import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.publishing.PublishingClients;
import com.praxis.mdp.publishing.internal.kafka.KafkaProducers;
import com.praxis.mdp.publishing.internal.rest.RestClients;
import com.praxis.mdp.publishing.internal.sftp.SftpSessions;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class DefaultPublishingClients implements PublishingClients {

  private final SftpSessions sftp;
  private final KafkaProducers kafka;
  private final RestClients rest;
  private final MdpProperties props;

  public DefaultPublishingClients(
      SftpSessions sftp, KafkaProducers kafka, RestClients rest, MdpProperties props) {
    this.sftp = sftp;
    this.kafka = kafka;
    this.rest = rest;
    this.props = props;
  }

  @Override
  public SftpSessions sftp() {
    return sftp;
  }

  @Override
  public KafkaProducers kafka() {
    return kafka;
  }

  @Override
  public RestClients rest() {
    return rest;
  }

  @Override
  public Path outputDir() {
    return props.getOutputDir();
  }

  @Override
  public Path workDir() {
    return props.getWorkDir();
  }
}
