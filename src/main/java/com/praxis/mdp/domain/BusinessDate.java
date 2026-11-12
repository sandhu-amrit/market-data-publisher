package com.praxis.mdp.domain;

import java.time.LocalDate;
import java.util.Objects;

/** A business date (ISO-8601 calendar date). Any past or current date is allowed. */
public record BusinessDate(LocalDate value) {

  public BusinessDate {
    Objects.requireNonNull(value, "value");
  }

  public static BusinessDate parse(String iso) {
    return new BusinessDate(LocalDate.parse(iso));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
