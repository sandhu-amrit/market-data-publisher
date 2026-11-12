package com.praxis.mdp.publishing.internal.sftp;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.publishing.MarketDataSink;
import com.praxis.mdp.publishing.SinkWriter;
import com.praxis.mdp.publishing.internal.file.LocalCsvWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpSession;

/** Stages a CSV locally, uploads it as {@code *.tmp}, then renames it on the server. */
public class SftpMarketDataSink implements MarketDataSink {

  private final MdpProperties.Sftp config;
  private final Path localDir;
  private final DefaultSftpSessionFactory sessions;

  public SftpMarketDataSink(MdpProperties props) {
    this.config = props.getDestinations().getSftp();
    this.localDir = props.getWorkDir().resolve("sftp");
    this.sessions = new DefaultSftpSessionFactory();
    sessions.setHost(config.getHost());
    sessions.setPort(config.getPort());
    sessions.setUser(config.getUsername());
    sessions.setPassword(config.getPassword());
    sessions.setAllowUnknownKeys(config.isAllowUnknownKeys());
    sessions.setTimeout((int) config.getTimeout().toMillis());
  }

  @Override
  public SinkWriter open(String jobName, InstrumentType type, RunId runId) {
    String fileName = type.name() + "_EOD_" + runId.businessDate() + ".csv";
    Path local = localDir.resolve(jobName).resolve("seq-" + runId.runSequence()).resolve(fileName);
    return new LocalCsvWriter(local, (temp, target) -> upload(temp, fileName));
  }

  private void upload(Path temp, String fileName) throws IOException {
    String remoteDir = config.getRemoteDirectory();
    String remoteFinal = join(remoteDir, fileName);
    String remoteTemp = remoteFinal + ".tmp";
    SftpSession session = sessions.getSession();
    try {
      ensureDirectory(session, remoteDir);
      if (session.exists(remoteTemp)) {
        session.remove(remoteTemp);
      }
      try (InputStream in = Files.newInputStream(temp)) {
        session.write(in, remoteTemp);
      }
      if (session.exists(remoteFinal)) {
        session.remove(remoteFinal);
      }
      session.rename(remoteTemp, remoteFinal);
    } finally {
      session.close();
      Files.deleteIfExists(temp);
    }
  }

  private static void ensureDirectory(SftpSession session, String remoteDir) {
    try {
      if (!session.exists(remoteDir)) {
        session.mkdir(remoteDir);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create remote directory " + remoteDir, e);
    }
  }

  private static String join(String dir, String name) {
    if (dir.endsWith("/")) {
      return dir + name;
    }
    return dir + "/" + name;
  }
}
