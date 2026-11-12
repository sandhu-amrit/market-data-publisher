package com.praxis.mdp.publishing.internal.sftp;

import com.praxis.mdp.infrastructure.config.Connections.SftpConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.stereotype.Component;

/** One session factory per SFTP profile, created on first use. */
@Component
public class SftpSessions {

  private final ConcurrentMap<SftpConnection, DefaultSftpSessionFactory> cache =
      new ConcurrentHashMap<>();

  public DefaultSftpSessionFactory get(SftpConnection connection) {
    if (!connection.enabled()) {
      throw new IllegalStateException("SFTP profile " + connection.host() + " is disabled");
    }
    return cache.computeIfAbsent(connection, SftpSessions::create);
  }

  public int size() {
    return cache.size();
  }

  private static DefaultSftpSessionFactory create(SftpConnection connection) {
    DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
    factory.setHost(connection.host());
    factory.setPort(connection.port());
    factory.setUser(connection.username());
    if (connection.password() != null && !connection.password().isBlank()) {
      factory.setPassword(connection.password());
    }
    if (connection.privateKey() != null && !connection.privateKey().isBlank()) {
      factory.setPrivateKey(new FileSystemResource(connection.privateKey()));
    }
    if (connection.knownHosts() != null && !connection.knownHosts().isBlank()) {
      factory.setKnownHostsResource(new FileSystemResource(connection.knownHosts()));
    }
    factory.setAllowUnknownKeys(connection.allowUnknownKeys());
    factory.setTimeout((int) connection.timeout().toMillis());
    return factory;
  }
}
