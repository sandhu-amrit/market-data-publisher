package com.praxis.mdp.pipeline.internal.flow;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.pipeline.RunParameters;
import com.praxis.mdp.source.MarketDataSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

/** Opens the source reader once the business date is known from the job parameters. */
final class SourceReader implements ItemStreamReader<MarketDataRecord>, StepExecutionListener {

  private final MarketDataSource source;
  private final InstrumentType type;
  private final int fetchSize;
  private ItemStreamReader<MarketDataRecord> delegate;

  SourceReader(MarketDataSource source, InstrumentType type, int fetchSize) {
    this.source = source;
    this.type = type;
    this.fetchSize = fetchSize;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    delegate =
        source.reader(
            type, RunParameters.businessDate(stepExecution.getJobParameters()), fetchSize);
  }

  @Override
  public void open(ExecutionContext executionContext) {
    delegate.open(executionContext);
  }

  @Override
  public MarketDataRecord read() throws Exception {
    return delegate.read();
  }

  @Override
  public void update(ExecutionContext executionContext) {
    delegate.update(executionContext);
  }

  @Override
  public void close() {
    if (delegate != null) {
      delegate.close();
    }
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    return null;
  }
}
