package com.praxis.mdp.pipeline.internal.validate;

/** The source row has no end-of-day price. Handling depends on {@code MissingPricePolicy}. */
public class MissingPriceException extends RuntimeException {

  public MissingPriceException(String instrumentId) {
    super("instrument " + instrumentId + " has no EOD price");
  }
}
