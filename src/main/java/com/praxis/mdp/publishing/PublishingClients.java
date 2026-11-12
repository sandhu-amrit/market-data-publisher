package com.praxis.mdp.publishing;

import com.praxis.mdp.publishing.internal.kafka.KafkaProducers;
import com.praxis.mdp.publishing.internal.rest.RestClients;
import com.praxis.mdp.publishing.internal.sftp.SftpSessions;
import java.nio.file.Path;

/** Lazy, cached clients. Callers must not ask for a client whose profile is disabled. */
public interface PublishingClients {

  SftpSessions sftp();

  KafkaProducers kafka();

  RestClients rest();

  Path outputDir();

  Path workDir();
}
