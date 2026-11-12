package com.praxis.mdp.source.internal.file;

import com.praxis.mdp.domain.EodPrice;
import com.praxis.mdp.domain.Instrument;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.infrastructure.config.Connections;
import com.praxis.mdp.source.AssetControlSource;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/**
 * File stand-in for Asset Control. Reads {@code <inputDir>/<pattern>} from the {@code assetControl}
 * source profile. A missing file fails the step.
 */
@Component
public class FileMarketDataSource implements AssetControlSource {

  static final String[] COLUMNS = {
    "instrumentId", "instrumentType", "symbol", "currency", "maturityDate", "closePrice"
  };

  private final Connections.SourceConnection connection;

  public FileMarketDataSource(Connections connections) {
    this.connection = connections.source().assetControl();
  }

  public Path fileFor(InstrumentType type, LocalDate businessDate) {
    String name =
        connection
            .filePattern()
            .replace("{type}", type.name())
            .replace("{businessDate}", businessDate.toString())
            .replace("{date}", businessDate.toString());
    return connection.inputDir().resolve(name);
  }

  @Override
  public ItemStreamReader<MarketDataRecord> reader(
      InstrumentType type, LocalDate businessDate, int fetchSize) {
    return new FlatFileItemReaderBuilder<MarketDataRecord>()
        .name("fileMarketDataReader-" + type + "-" + fetchSize)
        .resource(new FileSystemResource(fileFor(type, businessDate)))
        .linesToSkip(1)
        .delimited()
        .names(COLUMNS)
        .fieldSetMapper(fs -> map(fs, businessDate))
        .strict(true)
        .saveState(false)
        .build();
  }

  private static MarketDataRecord map(FieldSet fs, LocalDate businessDate) {
    String maturity = fs.readString("maturityDate");
    String price = fs.readString("closePrice");
    Instrument instrument =
        new Instrument(
            fs.readString("instrumentId"),
            InstrumentType.valueOf(fs.readString("instrumentType")),
            fs.readString("symbol"),
            fs.readString("currency"),
            maturity.isBlank() ? null : LocalDate.parse(maturity));
    BigDecimal closePrice = price.isBlank() ? null : new BigDecimal(price);
    return new MarketDataRecord(instrument, new EodPrice(businessDate, closePrice));
  }
}
