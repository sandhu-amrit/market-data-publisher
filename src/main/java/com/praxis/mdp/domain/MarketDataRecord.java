package com.praxis.mdp.domain;

/** Canonical record flowing through a run: reference data plus its EOD price. */
public record MarketDataRecord(Instrument instrument, EodPrice price) {}
