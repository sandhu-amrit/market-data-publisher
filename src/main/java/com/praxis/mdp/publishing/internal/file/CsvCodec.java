package com.praxis.mdp.publishing.internal.file;

import com.praxis.mdp.domain.MarketDataRecord;

/** CSV layout shared by the file and SFTP sinks. Matches the staged snapshot columns. */
public final class CsvCodec {

  public static final String HEADER =
      "businessDate,instrumentId,instrumentType,symbol,currency,maturityDate,closePrice";

  private CsvCodec() {}

  public static String line(MarketDataRecord record) {
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

  private static String csv(Object value) {
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
