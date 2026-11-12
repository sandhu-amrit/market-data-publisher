package com.praxis.mdp.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** End-of-day price. {@code closePrice} may be null when the source had none. */
public record EodPrice(LocalDate businessDate, BigDecimal closePrice) {}
