package com.praxis.mdp.pipeline.internal.validate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.praxis.mdp.domain.EodPrice;
import com.praxis.mdp.domain.Instrument;
import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MarketDataValidatorTest {

  private final MarketDataValidator validator = new MarketDataValidator(InstrumentType.FX_FUTURE);
  private final LocalDate date = LocalDate.parse("2026-11-11");

  @Test
  void acceptsACompleteRecord() {
    MarketDataRecord record = record("FXF-1", "1.0");
    assertThat(validator.process(record)).isSameAs(record);
  }

  @Test
  void rejectsAMissingPrice() {
    assertThatThrownBy(() -> validator.process(record("FXF-1", null)))
        .isInstanceOf(MissingPriceException.class);
  }

  @Test
  void rejectsANonPositivePrice() {
    assertThatThrownBy(() -> validator.process(record("FXF-1", "-1")))
        .isInstanceOf(InvalidRecordException.class);
  }

  @Test
  void rejectsABadCurrency() {
    MarketDataRecord record =
        new MarketDataRecord(
            new Instrument("FXF-1", InstrumentType.FX_FUTURE, "6EZ6", "US", date),
            new EodPrice(date, BigDecimal.ONE));
    assertThatThrownBy(() -> validator.process(record)).isInstanceOf(InvalidRecordException.class);
  }

  private MarketDataRecord record(String id, String price) {
    return new MarketDataRecord(
        new Instrument(id, InstrumentType.FX_FUTURE, "6EZ6", "USD", date),
        new EodPrice(date, price == null ? null : new BigDecimal(price)));
  }
}
