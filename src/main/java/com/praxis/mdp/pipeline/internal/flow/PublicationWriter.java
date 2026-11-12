package com.praxis.mdp.pipeline.internal.flow;

import com.praxis.mdp.domain.InstrumentType;
import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.pipeline.RunParameters;
import com.praxis.mdp.publishing.Publication;
import com.praxis.mdp.publishing.PublishRequest;
import com.praxis.mdp.publishing.PublishingClients;
import com.praxis.mdp.publishing.SinkWriter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;

final class PublicationWriter implements ItemStreamWriter<MarketDataRecord>, StepExecutionListener {

  private final Publication publication;
  private final PublishingClients clients;
  private final String jobName;
  private final InstrumentType type;
  private SinkWriter delegate;

  PublicationWriter(
      Publication publication, PublishingClients clients, String jobName, InstrumentType type) {
    this.publication = publication;
    this.clients = clients;
    this.jobName = jobName;
    this.type = type;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    delegate =
        publication.open(
            new PublishRequest(
                jobName,
                type,
                RunParameters.runId(stepExecution.getJobParameters()),
                publication.format()),
            clients);
  }

  @Override
  public void open(ExecutionContext executionContext) {
    delegate.open(executionContext);
  }

  @Override
  public void write(Chunk<? extends MarketDataRecord> chunk) throws Exception {
    delegate.write(chunk);
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
    if (delegate == null) {
      return null;
    }
    try {
      delegate.finish(stepExecution.getStatus() == BatchStatus.COMPLETED);
    } catch (RuntimeException e) {
      stepExecution.addFailureException(e);
      stepExecution.setStatus(BatchStatus.FAILED);
      return ExitStatus.FAILED.addExitDescription(e);
    }
    return null;
  }
}
