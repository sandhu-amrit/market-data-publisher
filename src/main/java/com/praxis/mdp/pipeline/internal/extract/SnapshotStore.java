package com.praxis.mdp.pipeline.internal.extract;

import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.pipeline.internal.transform.CsvSerializer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.stereotype.Component;

/**
 * Per-run staging file. Publish steps read it back so each destination sees the same validated
 * snapshot.
 */
@Component
public class SnapshotStore {

  private static final Logger log = LoggerFactory.getLogger(SnapshotStore.class);

  private final MdpProperties props;
  private final CsvSerializer serializer;

  public SnapshotStore(MdpProperties props, CsvSerializer serializer) {
    this.props = props;
    this.serializer = serializer;
  }

  public Path snapshot(String jobName, RunId runId) {
    return props
        .getWorkDir()
        .resolve(jobName)
        .resolve(runId.businessDate().toString())
        .resolve("seq-" + runId.runSequence())
        .resolve("snapshot.csv");
  }

  public SnapshotWriter writer(Path snapshot) {
    return new SnapshotWriter(snapshot, serializer);
  }

  public static final class SnapshotWriter implements ItemStreamWriter<MarketDataRecord> {

    private final Path target;
    private final Path temp;
    private final CsvSerializer serializer;
    private BufferedWriter out;
    private boolean opened;
    private long written;

    SnapshotWriter(Path target, CsvSerializer serializer) {
      this.target = target;
      this.temp = target.resolveSibling(target.getFileName() + ".tmp");
      this.serializer = serializer;
    }

    @Override
    public void open(ExecutionContext executionContext) {
      try {
        Files.createDirectories(target.getParent());
        out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8);
        out.write(serializer.header());
        out.newLine();
        opened = true;
      } catch (IOException e) {
        throw new UncheckedIOException("Cannot open snapshot " + temp, e);
      }
    }

    @Override
    public void write(Chunk<? extends MarketDataRecord> chunk) {
      try {
        for (MarketDataRecord record : chunk) {
          out.write(serializer.serialize(record));
          out.newLine();
          written++;
        }
        out.flush();
      } catch (IOException e) {
        throw new UncheckedIOException("Cannot write snapshot " + temp, e);
      }
    }

    @Override
    public void update(ExecutionContext executionContext) {}

    @Override
    public void close() {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        } finally {
          out = null;
        }
      }
    }

    public void finish(boolean success) {
      close();
      if (!opened) {
        return;
      }
      try {
        if (!success) {
          Files.deleteIfExists(temp);
          log.warn("Extract failed; discarded partial snapshot {}", temp);
          return;
        }
        move(temp, target);
        log.info("Staged {} records at {}", written, target);
      } catch (IOException e) {
        throw new UncheckedIOException("Cannot publish snapshot " + target, e);
      }
    }

    private static void move(Path from, Path to) throws IOException {
      try {
        Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }
}
