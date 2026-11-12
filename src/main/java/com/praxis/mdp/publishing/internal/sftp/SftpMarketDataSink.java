package com.praxis.mdp.publishing.internal.sftp;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.infrastructure.config.Connections.SftpConnection;
import com.praxis.mdp.publishing.MarketDataSink;
import com.praxis.mdp.publishing.OutputNames;
import com.praxis.mdp.publishing.SinkWriter;
import com.praxis.mdp.publishing.internal.file.LocalCsvWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.integration.sftp.session.SftpSession;

/** Stages a CSV locally, uploads it as {@code *.tmp}, then renames it on the server. */
public class SftpMarketDataSink implements MarketDataSink {

  private final SftpConnection connection;
  private final Path workDir;
  private final String remoteDir;
  private final String filePattern;
  private final SftpSessions sessions;

  public SftpMarketDataSink(
      SftpConnection connection, Path workDir, String remoteDir, String filePattern) {
    this(connection, workDir, remoteDir, filePattern, new SftpSessions());
  }

  public SftpMarketDataSink(
      SftpConnection connection,
      Path workDir,
      String remoteDir,
      String filePattern,
      SftpSessions sessions) {
    this.connection = connection;
    this.workDir = workDir;
    this.remoteDir = remoteDir;
    this.filePattern = filePattern;
    this.sessions = sessions;
  }

  @Override
  public SinkWriter open(String jobName, InstrumentType type, RunId runId) {
    String fileName = OutputNames.apply(filePattern, jobName, type, runId);
    Path local =
        workDir
            .resolve("sftp")
            .resolve(jobName)
            .resolve("seq-" + runId.runSequence())
            .resolve(fileName);
    return new LocalCsvWriter(local, (temp, target) -> upload(temp, fileName));
  }

  private void upload(Path temp, String fileName) throws IOException {
    SftpSession session = sessions.get(connection).getSession();
    String remoteFinal = join(remoteDir, fileName);
    String remoteTemp = remoteFinal + ".tmp";
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
