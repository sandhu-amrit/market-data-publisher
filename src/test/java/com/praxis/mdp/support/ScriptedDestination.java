package com.praxis.mdp.support;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.RunId;
import com.praxis.mdp.publishing.Destination;
import com.praxis.mdp.publishing.MarketDataSink;
import com.praxis.mdp.publishing.SinkWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

/** Test double destination. {@code onWrite} runs for every chunk. */
public final class ScriptedDestination implements Destination, MarketDataSink {

  private final String name;
  private final int order;
  private final Runnable onWrite;

  public ScriptedDestination(String name, int order, Runnable onWrite) {
    this.name = name;
    this.order = order;
    this.onWrite = onWrite;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int order() {
    return order;
  }

  @Override
  public MarketDataSink sink() {
    return this;
  }

  @Override
  public SinkWriter open(String jobName, InstrumentType type, RunId runId) {
    return new SinkWriter() {
      @Override
      public void open(ExecutionContext executionContext) {}

      @Override
      public void write(Chunk<? extends MarketDataRecord> chunk) {
        onWrite.run();
      }

      @Override
      public void update(ExecutionContext executionContext) {}

      @Override
      public void close() {}

      @Override
      public void finish(boolean success) {}
    };
  }
}
