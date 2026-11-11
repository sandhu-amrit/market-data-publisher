package com.praxis.mdp.batch;

import com.praxis.mdp.model.InstrumentType;
import com.praxis.mdp.model.MarketDataRecord;
import com.praxis.mdp.source.MarketDataSource;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

/**
 * Creates the real reader just before the step starts, once the business date is known from the job
 * parameters. This lets one job definition serve every business date.
 */
class LazyReader implements ItemStreamReader<MarketDataRecord>, StepExecutionListener {

    private final MarketDataSource source;
    private final InstrumentType type;
    private ItemStreamReader<MarketDataRecord> delegate;

    LazyReader(MarketDataSource source, InstrumentType type) {
        this.source = source;
        this.type = type;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        delegate = source.reader(type, RunParameters.businessDate(stepExecution.getJobParameters()));
    }

    @Override
    public void open(ExecutionContext ctx) {
        delegate.open(ctx);
    }

    @Override
    public MarketDataRecord read() throws Exception {
        return delegate.read();
    }

    @Override
    public void update(ExecutionContext ctx) {
        delegate.update(ctx);
    }

    @Override
    public void close() {
        if (delegate != null) {
            delegate.close();
        }
    }
}
