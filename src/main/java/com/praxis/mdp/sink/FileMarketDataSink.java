package com.praxis.mdp.sink;

import com.praxis.mdp.config.MdpProperties;
import com.praxis.mdp.model.InstrumentType;
import com.praxis.mdp.model.MarketDataRecord;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Writes {@code <outputDir>/<INSTRUMENT_TYPE>_EOD_<yyyy-MM-dd>.csv}. Data goes to a ".tmp" file first and is
 * renamed atomically only when the step succeeds, so consumers never see a half-written file. A rerun of the
 * same date simply overwrites the previous file.
 */
@Component
public class FileMarketDataSink implements MarketDataSink {

    private final Path outputDir;

    public FileMarketDataSink(MdpProperties props) {
        this.outputDir = props.outputDir();
    }

    public Path finalFile(InstrumentType type, LocalDate businessDate) {
        return outputDir.resolve(type.name() + "_EOD_" + businessDate + ".csv");
    }

    @Override
    public SinkWriter writer(InstrumentType type, LocalDate businessDate) {
        return new FileSinkWriter(finalFile(type, businessDate));
    }

    static final class FileSinkWriter implements SinkWriter {

        private static final Logger log = LoggerFactory.getLogger(FileSinkWriter.class);
        private static final String HEADER = "businessDate,instrumentId,instrumentType,symbol,currency,maturityDate,closePrice";

        private final Path target;
        private final Path temp;
        private BufferedWriter out;
        private long written;

        FileSinkWriter(Path target) {
            this.target = target;
            this.temp = target.resolveSibling(target.getFileName() + ".tmp");
        }

        @Override
        public void open(ExecutionContext executionContext) {
            try {
                Files.createDirectories(target.getParent());
                out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8); // truncates leftovers
                out.write(HEADER);
                out.newLine();
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot open " + temp, e);
            }
        }

        @Override
        public void write(Chunk<? extends MarketDataRecord> chunk) {
            try {
                for (MarketDataRecord r : chunk) {
                    out.write(String.join(",",
                            csv(r.price().businessDate()), csv(r.instrument().id()), csv(r.instrument().type()),
                            csv(r.instrument().symbol()), csv(r.instrument().currency()),
                            csv(r.instrument().maturityDate()), csv(r.price().closePrice())));
                    out.newLine();
                    written++;
                }
                out.flush();
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot write " + temp, e);
            }
        }

        @Override
        public void finish(boolean success) {
            close();
            try {
                if (!success) {
                    Files.deleteIfExists(temp);
                    log.warn("Run failed; discarded partial output {}", temp);
                    return;
                }
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("Published {} records to {}", written, target);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot publish " + target, e);
            }
        }

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

        private static String csv(Object value) {
            if (value == null) {
                return "";
            }
            String s = value.toString();
            return (s.contains(",") || s.contains("\"") || s.contains("\n"))
                    ? "\"" + s.replace("\"", "\"\"") + "\""
                    : s;
        }
    }
}
