package com.praxis.mdp.publishing.internal.file;

import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.publishing.SinkWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

/**
 * Writes a CSV to a temp file and, on success, hands that file to {@link Publisher}. The publisher
 * either renames it into place or uploads it.
 */
public final class LocalCsvWriter implements SinkWriter {

  private static final Logger log = LoggerFactory.getLogger(LocalCsvWriter.class);

  @FunctionalInterface
  public interface Publisher {
    void publish(Path tempFile, Path target) throws IOException;
  }

  private final Path target;
  private final Path temp;
  private final Publisher publisher;
  private BufferedWriter out;
  private boolean opened;
  private long written;

  public LocalCsvWriter(Path target, Publisher publisher) {
    this.target = target;
    this.temp = target.resolveSibling(target.getFileName() + ".tmp");
    this.publisher = publisher;
  }

  @Override
  public void open(ExecutionContext executionContext) {
    try {
      if (target.getParent() != null) {
        Files.createDirectories(target.getParent());
      }
      out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8);
      out.write(CsvCodec.HEADER);
      out.newLine();
      opened = true;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot open " + temp, e);
    }
  }

  @Override
  public void write(Chunk<? extends MarketDataRecord> chunk) {
    try {
      for (MarketDataRecord record : chunk) {
        out.write(CsvCodec.line(record));
        out.newLine();
        written++;
      }
      out.flush();
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write " + temp, e);
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

  @Override
  public void finish(boolean success) {
    close();
    if (!opened) {
      return;
    }
    try {
      if (!success) {
        Files.deleteIfExists(temp);
        log.warn("Discarded partial output {}", temp);
        return;
      }
      publisher.publish(temp, target);
      log.info("Published {} records to {}", written, target);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot publish " + target, e);
    }
  }
}
