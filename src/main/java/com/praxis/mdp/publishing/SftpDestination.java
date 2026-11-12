package com.praxis.mdp.publishing;

import com.praxis.mdp.infrastructure.config.Connections.SftpConnection;
import com.praxis.mdp.publishing.internal.sftp.SftpMarketDataSink;

/** SFTP publication bound to one {@link SftpConnection} profile. */
public final class SftpDestination {

  private SftpDestination() {}

  public static Builder to(SftpConnection connection) {
    return new Builder(connection);
  }

  public static final class Builder implements Publication {
    private final SftpConnection connection;
    private String remoteDir = "upload";
    private String fileName = "{type}_EOD_{businessDate}_{runSequence}.csv";
    private Format format = Format.CSV;

    private Builder(SftpConnection connection) {
      if (connection == null) {
        throw new IllegalArgumentException("SFTP connection is required");
      }
      this.connection = connection;
    }

    public Builder remoteDir(String remoteDir) {
      this.remoteDir = remoteDir;
      return this;
    }

    public String remoteDir() {
      return remoteDir;
    }

    public Builder fileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public Builder format(Format format) {
      this.format = format;
      return this;
    }

    public SftpConnection connection() {
      return connection;
    }

    @Override
    public String stepName() {
      return "sftp";
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
      return new SftpMarketDataSink(
              connection, clients.workDir(), remoteDir, fileName, clients.sftp())
          .open(request.jobName(), request.type(), request.runId());
    }
  }
}
