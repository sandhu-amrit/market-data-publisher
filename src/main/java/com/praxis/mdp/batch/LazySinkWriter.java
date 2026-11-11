package com.praxis.mdp.batch;

import com.praxis.mdp.model.InstrumentType;
import com.praxis.mdp.model.MarketDataRecord;
import com.praxis.mdp.sink.MarketDataSink;
import com.praxis.mdp.sink.SinkWriter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;

/** Writer counterpart of {@link LazyReader}; also tells the sink whether the step succeeded when it ends. */
class LazySinkWriter implements ItemStreamWriter<MarketDataRecord>, StepExecutionListener {

    private final MarketDataSink sink;
    private final InstrumentType type;
    private SinkWriter delegate;

    LazySinkWriter(MarketDataSink sink, InstrumentType type) {
        this.sink = sink;
        this.type = type;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        delegate = sink.writer(type, RunParameters.businessDate(stepExecution.getJobParameters()));
    }

    @Override
    public void open(ExecutionContext ctx) {
        delegate.open(ctx);
    }

    @Override
    public void write(Chunk<? extends MarketDataRecord> chunk) throws Exception {
        delegate.write(chunk);
    }

    @Override
    public void update(ExecutionContext ctx) {
        delegate.update(ctx);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (delegate != null) {
            delegate.finish(stepExecution.getStatus() == BatchStatus.COMPLETED);
        }
        return null;
    }

    @Override
    public void close() {
        if (delegate != null) {
            delegate.close();
        }
    }
}
