package com.praxis.mdp.pipeline;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.policy.FailurePolicy;
import com.praxis.mdp.domain.policy.MissingPricePolicy;
import com.praxis.mdp.publishing.Publication;
import com.praxis.mdp.source.MarketDataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * One end-of-day job. Source, destinations, formats, and policies are fixed here. Cron and batch
 * tuning come from configuration at startup.
 */
public final class JobDefinition {

  private final String name;
  private final InstrumentType instrumentType;
  private final MarketDataSource source;
  private final MissingPricePolicy missingPrices;
  private final FailurePolicy onFailure;
  private final List<Publication> publications;

  private JobDefinition(Builder builder) {
    this.name = builder.name;
    this.instrumentType = builder.instrumentType;
    this.source = builder.source;
    this.missingPrices = builder.missingPrices;
    this.onFailure = builder.onFailure;
    this.publications = List.copyOf(builder.publications);
  }

  public static Builder builder(String name) {
    return new Builder(name);
  }

  public String name() {
    return name;
  }

  public InstrumentType instrumentType() {
    return instrumentType;
  }

  public MarketDataSource source() {
    return source;
  }

  public MissingPricePolicy missingPrices() {
    return missingPrices;
  }

  public FailurePolicy onFailure() {
    return onFailure;
  }

  public List<Publication> publications() {
    return publications;
  }

  public static final class Builder {
    private final String name;
    private InstrumentType instrumentType;
    private MarketDataSource source;
    private MissingPricePolicy missingPrices = MissingPricePolicy.SKIP_AND_ALERT;
    private FailurePolicy onFailure = FailurePolicy.FAIL_FAST;
    private final List<Publication> publications = new ArrayList<>();

    private Builder(String name) {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("job name is required");
      }
      this.name = name;
    }

    public Builder instrumentType(InstrumentType instrumentType) {
      this.instrumentType = instrumentType;
      return this;
    }

    public Builder source(MarketDataSource source) {
      this.source = source;
      return this;
    }

    public Builder missingPrices(MissingPricePolicy missingPrices) {
      this.missingPrices = missingPrices;
      return this;
    }

    public Builder onFailure(FailurePolicy onFailure) {
      this.onFailure = onFailure;
      return this;
    }

    public Builder publishTo(Publication publication) {
      if (publication == null) {
        throw new IllegalArgumentException("publication is required");
      }
      this.publications.add(publication);
      return this;
    }

    public JobDefinition build() {
      if (instrumentType == null) {
        throw new IllegalStateException("Job " + name + " has no instrument type");
      }
      if (source == null) {
        throw new IllegalStateException("Job " + name + " has no source");
      }
      if (publications.isEmpty()) {
        throw new IllegalStateException("Job " + name + " publishes nowhere");
      }
      return new JobDefinition(this);
    }
  }
}
