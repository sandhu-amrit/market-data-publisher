package com.praxis.mdp.pipeline.internal.validate;

import static org.assertj.core.api.Assertions.assertThat;

import com.praxis.mdp.domain.policy.MissingPricePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.FlatFileParseException;

class MdpSkipPolicyTest {

  @Test
  void missingPricesDoNotConsumeTheInvalidBudget() {
    MdpSkipPolicy policy = new MdpSkipPolicy(MissingPricePolicy.SKIP_AND_ALERT, 1);
    assertThat(policy.shouldSkip(new MissingPriceException("a"), 0)).isTrue();
    assertThat(policy.shouldSkip(new MissingPriceException("b"), 1)).isTrue();
    assertThat(policy.shouldSkip(new InvalidRecordException("bad"), 2)).isTrue();
    assertThat(policy.shouldSkip(new InvalidRecordException("worse"), 3)).isFalse();
  }

  @Test
  void failRunDoesNotSkipMissingPrices() {
    MdpSkipPolicy policy = new MdpSkipPolicy(MissingPricePolicy.FAIL_RUN, 10);
    assertThat(policy.shouldSkip(new MissingPriceException("a"), 0)).isFalse();
    assertThat(policy.shouldSkip(new FlatFileParseException("bad line", "x"), 0)).isTrue();
  }
}
