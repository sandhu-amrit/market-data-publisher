package com.praxis.mdp.model;

import java.time.LocalDate;

/** Reference data for one instrument. */
public record Instrument(
        String id,
        InstrumentType type,
        String symbol,
        String currency,
        LocalDate maturityDate) {
}
