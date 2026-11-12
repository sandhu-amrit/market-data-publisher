package com.praxis.mdp.pipeline.internal.transform;

import com.praxis.mdp.domain.EodPrice;
import com.praxis.mdp.domain.Instrument;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
public class CsvSerializer implements Serializer {

  public static final String[] COLUMNS = {
    "businessDate",
    "instrumentId",
    "instrumentType",
    "symbol",
    "currency",
    "maturityDate",
    "closePrice"
  };

  @Override
  public String format() {
    return "csv";
  }

  @Override
  public String header() {
    return String.join(",", COLUMNS);
  }

  @Override
  public String serialize(MarketDataRecord record) {
    return String.join(
        ",",
        csv(record.price().businessDate()),
        csv(record.instrument().id()),
        csv(record.instrument().type()),
        csv(record.instrument().symbol()),
        csv(record.instrument().currency()),
        csv(record.instrument().maturityDate()),
        csv(record.price().closePrice()));
  }

  public ItemStreamReader<MarketDataRecord> reader(Path snapshot, String name) {
    return new FlatFileItemReaderBuilder<MarketDataRecord>()
        .name(name)
        .resource(new FileSystemResource(snapshot))
        .linesToSkip(1)
        .delimited()
        .names(COLUMNS)
        .fieldSetMapper(CsvSerializer::parse)
        .strict(true)
        .saveState(false)
        .build();
  }

  static MarketDataRecord parse(FieldSet fs) {
    String maturity = fs.readString("maturityDate");
    String price = fs.readString("closePrice");
    LocalDate businessDate = LocalDate.parse(fs.readString("businessDate"));
    Instrument instrument =
        new Instrument(
            fs.readString("instrumentId"),
            InstrumentType.valueOf(fs.readString("instrumentType")),
            fs.readString("symbol"),
            fs.readString("currency"),
            maturity.isBlank() ? null : LocalDate.parse(maturity));
    BigDecimal close = price.isBlank() ? null : new BigDecimal(price);
    return new MarketDataRecord(instrument, new EodPrice(businessDate, close));
  }

  static String csv(Object value) {
    if (value == null) {
      return "";
    }
    String text = value.toString();
    if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
      return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    return text;
  }
}
