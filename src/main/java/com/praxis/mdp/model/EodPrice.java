package com.praxis.mdp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** End-of-day price for one instrument on one business date. {@code closePrice} may be null if the source had none. */
public record EodPrice(LocalDate businessDate, BigDecimal closePrice) {
}
