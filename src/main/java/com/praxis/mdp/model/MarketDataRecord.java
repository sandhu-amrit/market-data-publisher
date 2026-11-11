package com.praxis.mdp.model;

/** The canonical unit that flows through every job: reference data plus its EOD price. */
public record MarketDataRecord(Instrument instrument, EodPrice price) {
}
