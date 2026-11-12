package com.praxis.mdp.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Identity of one logical run. Identifying job parameters are {@code businessDate} and {@code
 * runSequence}. An overwrite rerun of the same date allocates the next sequence.
 */
public record RunId(BusinessDate businessDate, long runSequence) {

  public RunId {
    Objects.requireNonNull(businessDate, "businessDate");
    if (runSequence < 1) {
      throw new IllegalArgumentException("runSequence must be >= 1");
    }
  }

  public static RunId of(LocalDate businessDate, long runSequence) {
    return new RunId(new BusinessDate(businessDate), runSequence);
  }
}
