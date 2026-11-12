package com.praxis.mdp.pipeline.internal.validate;

import com.praxis.mdp.domain.policy.MissingPricePolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;

/**
 * Missing prices are skipped only for {@link MissingPricePolicy#SKIP_AND_ALERT} and do not consume
 * the invalid-record budget. Other invalid or unreadable rows are skipped until {@code skipLimit}.
 */
public class MdpSkipPolicy implements SkipPolicy {

  private final MissingPricePolicy missingPricePolicy;
  private final int skipLimit;
  private int invalidSkips;

  public MdpSkipPolicy(MissingPricePolicy missingPricePolicy, int skipLimit) {
    this.missingPricePolicy = missingPricePolicy;
    this.skipLimit = skipLimit;
  }

  @Override
  public boolean shouldSkip(Throwable t, long skipCount) {
    Throwable cause = unwrap(t);
    if (cause instanceof MissingPriceException) {
      return missingPricePolicy == MissingPricePolicy.SKIP_AND_ALERT;
    }
    if (cause instanceof InvalidRecordException || cause instanceof FlatFileParseException) {
      if (invalidSkips >= skipLimit) {
        return false;
      }
      invalidSkips++;
      return true;
    }
    return false;
  }

  private static Throwable unwrap(Throwable t) {
    Throwable current = t;
    while (current.getCause() != null && current.getCause() != current) {
      if (current instanceof MissingPriceException
          || current instanceof InvalidRecordException
          || current instanceof FlatFileParseException) {
        return current;
      }
      current = current.getCause();
    }
    return current;
  }
}
