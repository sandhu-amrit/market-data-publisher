package com.praxis.mdp.source.internal.file;

import com.praxis.mdp.domain.EodPrice;
import com.praxis.mdp.domain.Instrument;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.source.MarketDataSource;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/**
 * File stand-in for Asset Control. Reads {@code <inputDir>/<INSTRUMENT_TYPE>_<yyyy-MM-dd>.csv}. A
 * missing file fails the step. Malformed lines surface as parse exceptions that the extract step
 * skips.
 */
@Component
@ConditionalOnProperty(
    prefix = "mdp.source",
    name = "type",
    havingValue = "file",
    matchIfMissing = true)
public class FileMarketDataSource implements MarketDataSource {

  static final String[] COLUMNS = {
    "instrumentId", "instrumentType", "symbol", "currency", "maturityDate", "closePrice"
  };

  private final MdpProperties props;

  public FileMarketDataSource(MdpProperties props) {
    this.props = props;
  }

  public Path fileFor(InstrumentType type, LocalDate businessDate) {
    return props.getInputDir().resolve(type.name() + "_" + businessDate + ".csv");
  }

  @Override
  public ItemStreamReader<MarketDataRecord> reader(InstrumentType type, LocalDate businessDate) {
    return new FlatFileItemReaderBuilder<MarketDataRecord>()
        .name("fileMarketDataReader-" + type)
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
