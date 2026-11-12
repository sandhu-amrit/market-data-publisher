package com.praxis.mdp.pipeline.internal.transform;

import com.praxis.mdp.domain.MarketDataRecord;
import org.springframework.stereotype.Component;

@Component
public class IdentityTransformer implements Transformer {

  @Override
  public MarketDataRecord transform(MarketDataRecord record) {
    return record;
  }
}
