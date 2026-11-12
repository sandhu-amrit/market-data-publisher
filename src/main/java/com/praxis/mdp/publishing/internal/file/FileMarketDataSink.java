package com.praxis.mdp.publishing.internal.file;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.publishing.MarketDataSink;
import com.praxis.mdp.publishing.SinkWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes {@code <outputDir>/<TYPE>_EOD_<date>.csv} via a temp file and an atomic rename. Reruns
 * overwrite.
 */
public class FileMarketDataSink implements MarketDataSink {

  private final Path outputDir;

  public FileMarketDataSink(Path outputDir) {
    this.outputDir = outputDir;
  }

  public Path finalFile(InstrumentType type, RunId runId) {
    return outputDir.resolve(type.name() + "_EOD_" + runId.businessDate() + ".csv");
  }

  public SinkWriter writerFor(Path target) {
    return new LocalCsvWriter(target, FileMarketDataSink::move);
  }

  @Override
  public SinkWriter open(String jobName, InstrumentType type, RunId runId) {
    return writerFor(finalFile(type, runId));
  }

  static void move(Path temp, Path target) throws IOException {
    try {
      Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
