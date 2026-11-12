package com.praxis.mdp.publishing.internal.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.praxis.mdp.TestDirs;
import com.praxis.mdp.domain.EodPrice;
import com.praxis.mdp.domain.Instrument;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.publishing.SinkWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

class FileMarketDataSinkTest {

  @Test
  void renamesAtomicallyOnSuccessAndDeletesTheTempFileOnFailure() throws Exception {
    Path dir = TestDirs.newTempDir("mdp-file-sink");
    FileMarketDataSink sink = new FileMarketDataSink(dir);
    RunId runId = RunId.of(LocalDate.parse("2026-11-11"), 1);
    MarketDataRecord record =
        new MarketDataRecord(
            new Instrument(
                "FXF-1", InstrumentType.FX_FUTURE, "6EZ6", "USD", LocalDate.parse("2026-12-14")),
            new EodPrice(runId.businessDate().value(), new BigDecimal("1.0842")));

    SinkWriter ok = sink.open("fxFuturesEodJob", InstrumentType.FX_FUTURE, runId);
    ok.open(new ExecutionContext());
    ok.write(new Chunk<>(List.of(record)));
    ok.finish(true);
    Path target = dir.resolve("FX_FUTURE_EOD_2026-11-11.csv");
    assertThat(target).exists();
    assertThat(Files.readAllLines(target).get(1))
        .isEqualTo("2026-11-11,FXF-1,FX_FUTURE,6EZ6,USD,2026-12-14,1.0842");
    assertThat(dir.resolve("FX_FUTURE_EOD_2026-11-11.csv.tmp")).doesNotExist();

    SinkWriter failed =
        sink.open(
            "fxFuturesEodJob", InstrumentType.FX_FUTURE, RunId.of(runId.businessDate().value(), 2));
    failed.open(new ExecutionContext());
    failed.write(new Chunk<>(List.of(record)));
    failed.finish(false);
    assertThat(dir.resolve("FX_FUTURE_EOD_2026-11-11.csv.tmp")).doesNotExist();
  }
}
