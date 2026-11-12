package com.praxis.mdp.pipeline.internal.validate;

import com.praxis.mdp.domain.Instrument;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import java.math.BigDecimal;
import org.springframework.batch.item.ItemProcessor;

/**
 * Structural checks. A missing price throws {@link MissingPriceException}; {@link MdpSkipPolicy}
 * decides whether that fails the run or is skipped and alerted.
 */
public class MarketDataValidator implements ItemProcessor<MarketDataRecord, MarketDataRecord> {

  private final InstrumentType expectedType;

  public MarketDataValidator(InstrumentType expectedType) {
    this.expectedType = expectedType;
  }

  @Override
  public MarketDataRecord process(MarketDataRecord record) {
    Instrument instrument = record.instrument();
    String id = instrument.id();
    if (id == null || id.isBlank()) {
      throw new InvalidRecordException("missing instrumentId");
    }
    if (instrument.type() != expectedType) {
      throw new InvalidRecordException(
          "instrument " + id + " has type " + instrument.type() + ", expected " + expectedType);
    }
    if (instrument.symbol() == null || instrument.symbol().isBlank()) {
      throw new InvalidRecordException("instrument " + id + " has no symbol");
    }
    String ccy = instrument.currency();
    if (ccy == null || !ccy.matches("[A-Z]{3}")) {
      throw new InvalidRecordException("instrument " + id + " has invalid currency '" + ccy + "'");
    }
    BigDecimal price = record.price().closePrice();
    if (price == null) {
      throw new MissingPriceException(id);
    }
    if (price.signum() <= 0) {
      throw new InvalidRecordException("instrument " + id + " has non-positive price " + price);
    }
    return record;
  }
}
