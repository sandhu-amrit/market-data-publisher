package com.praxis.mdp.batch;

import com.praxis.mdp.model.Instrument;
import com.praxis.mdp.model.InstrumentType;
import com.praxis.mdp.model.MarketDataRecord;
import java.math.BigDecimal;
import org.springframework.batch.item.ItemProcessor;

/** Basic checks on each record. A failing record throws {@link InvalidRecordException} and is skipped. */
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
            throw new InvalidRecordException("instrument " + id + " has type " + instrument.type() + ", expected " + expectedType);
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
            throw new InvalidRecordException("instrument " + id + " has no EOD price");
        }
        if (price.signum() <= 0) {
            throw new InvalidRecordException("instrument " + id + " has non-positive price " + price);
        }
        return record;
    }
}
