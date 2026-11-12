package com.praxis.mdp.pipeline.internal.flow;

import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.pipeline.RunParameters;
import com.praxis.mdp.pipeline.internal.extract.SnapshotStore;
import com.praxis.mdp.pipeline.internal.transform.CsvSerializer;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

final class SnapshotReader implements ItemStreamReader<MarketDataRecord>, StepExecutionListener {

  private final SnapshotStore snapshots;
  private final CsvSerializer serializer;
  private final String jobName;
  private final String stepName;
  private ItemStreamReader<MarketDataRecord> delegate;

  SnapshotReader(
      SnapshotStore snapshots, CsvSerializer serializer, String jobName, String stepName) {
    this.snapshots = snapshots;
    this.serializer = serializer;
    this.jobName = jobName;
    this.stepName = stepName;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    var path = snapshots.snapshot(jobName, RunParameters.runId(stepExecution.getJobParameters()));
    delegate = serializer.reader(path, stepName + "-snapshot");
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
