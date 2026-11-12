package com.praxis.mdp.pipeline.internal.flow;

import com.praxis.mdp.domain.MarketDataRecord;
import com.praxis.mdp.domain.policy.FailurePolicy;
import com.praxis.mdp.pipeline.BatchSettings;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.JobRuntime;
import com.praxis.mdp.pipeline.internal.extract.SnapshotStore;
import com.praxis.mdp.pipeline.internal.listener.MetricsStepListener;
import com.praxis.mdp.pipeline.internal.listener.RunLoggingListener;
import com.praxis.mdp.pipeline.internal.listener.SkipLoggingListener;
import com.praxis.mdp.pipeline.internal.transform.CsvSerializer;
import com.praxis.mdp.pipeline.internal.transform.Transformer;
import com.praxis.mdp.pipeline.internal.validate.MarketDataValidator;
import com.praxis.mdp.pipeline.internal.validate.MdpSkipPolicy;
import com.praxis.mdp.publishing.Publication;
import com.praxis.mdp.publishing.PublishingClients;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.ResourceAccessException;

/**
 * Extract once into a staging snapshot, then one publish step per enabled destination. {@link
 * FailurePolicy#FAIL_FAST} stops at the first destination failure. {@link
 * FailurePolicy#CONTINUE_OTHER_DESTINATIONS} runs every destination and fails the job afterwards if
 * any of them failed. Restart re-executes failed steps and skips completed ones.
 */
@Component
public class EodJobFactory {

  public static final String EXTRACT_STEP = "extract";
  public static final String FAILURE_GATE = "failureGate";

  private static final Logger log = LoggerFactory.getLogger(EodJobFactory.class);

  private final JobRepository jobRepository;
  private final PlatformTransactionManager txManager;
  private final SnapshotStore snapshots;
  private final CsvSerializer serializer;
  private final Transformer transformer;
  private final PublishingClients clients;
  private final JobRuntime runtime;
  private final RunLoggingListener runLoggingListener;
  private final SkipLoggingListener skipLoggingListener;
  private final MetricsStepListener metricsStepListener;

  public EodJobFactory(
      JobRepository jobRepository,
      PlatformTransactionManager txManager,
      SnapshotStore snapshots,
      CsvSerializer serializer,
      Transformer transformer,
      PublishingClients clients,
      JobRuntime runtime,
      RunLoggingListener runLoggingListener,
      SkipLoggingListener skipLoggingListener,
      MetricsStepListener metricsStepListener) {
    this.jobRepository = jobRepository;
    this.txManager = txManager;
    this.snapshots = snapshots;
    this.serializer = serializer;
    this.transformer = transformer;
    this.clients = clients;
    this.runtime = runtime;
    this.runLoggingListener = runLoggingListener;
    this.skipLoggingListener = skipLoggingListener;
    this.metricsStepListener = metricsStepListener;
  }

  public Job create(JobDefinition def) {
    BatchSettings batch = runtime.batch(def.name());
    Step extract = extractStep(def, batch);
    List<Publication> selected = def.publications().stream().filter(Publication::active).toList();
    List<Step> publish = new ArrayList<>();
    for (Publication publication : selected) {
      publish.add(publishStep(def, publication, batch));
    }
    log.info(
        "Job {} instrument={} failurePolicy={} missingPrices={} chunkSize={} threads={} destinations={}",
        def.name(),
        def.instrumentType(),
        def.onFailure(),
        def.missingPrices(),
        batch.chunkSize(),
        batch.threads(),
        selected.stream().map(Publication::stepName).toList());
    if (def.onFailure() == FailurePolicy.CONTINUE_OTHER_DESTINATIONS && !publish.isEmpty()) {
      return continueJob(def, extract, publish);
    }
    return failFastJob(def, extract, publish);
  }

  private Step extractStep(JobDefinition def, BatchSettings batch) {
    SourceReader reader = new SourceReader(def.source(), def.instrumentType(), batch.fetchSize());
    StagingWriter writer = new StagingWriter(snapshots, def.name());
    var builder =
        new StepBuilder(EXTRACT_STEP, jobRepository)
            .<MarketDataRecord, MarketDataRecord>chunk(batch.chunkSize(), txManager)
            .reader(reader)
            .processor(new MarketDataValidator(def.instrumentType()))
            .writer(writer)
            .faultTolerant()
            .skipPolicy(new MdpSkipPolicy(def.missingPrices(), batch.skipLimit()));
    if (batch.retryLimit() > 0) {
      builder
          .retryLimit(batch.retryLimit())
          .retry(IOException.class)
          .retry(ResourceAccessException.class);
    }
    return builder.listener(skipLoggingListener).listener(metricsStepListener).build();
  }

  private Step publishStep(JobDefinition def, Publication publication, BatchSettings batch) {
    String stepName = "publish-" + publication.stepName();
    SnapshotReader reader = new SnapshotReader(snapshots, serializer, def.name(), stepName);
    PublicationWriter writer =
        new PublicationWriter(publication, clients, def.name(), def.instrumentType());
    return new StepBuilder(stepName, jobRepository)
        .<MarketDataRecord, MarketDataRecord>chunk(batch.chunkSize(), txManager)
        .reader(reader)
        .processor(item -> transformer.transform(item))
        .writer(writer)
        .listener(metricsStepListener)
        .build();
  }

  private Step failureGate(List<String> publishStepNames) {
    return new StepBuilder(FAILURE_GATE, jobRepository)
        .tasklet(
            (contribution, chunkContext) -> {
              var execution = chunkContext.getStepContext().getStepExecution().getJobExecution();
              List<String> failed = new ArrayList<>();
              for (var se : execution.getStepExecutions()) {
                if (!publishStepNames.contains(se.getStepName())) {
                  continue;
                }
                // A continued failure is upgraded to ABANDONED when the next step starts.
                // Put it back to FAILED so a restart re-runs that destination.
                if (se.getStatus() == BatchStatus.ABANDONED) {
                  se.setStatus(BatchStatus.FAILED);
                  jobRepository.update(se);
                }
                if (se.getStatus() == BatchStatus.FAILED) {
                  failed.add(se.getStepName());
                }
              }
              if (!failed.isEmpty()) {
                contribution.setExitStatus(
                    ExitStatus.FAILED.addExitDescription("Destinations failed: " + failed));
                throw new IllegalStateException("Destinations failed: " + failed);
              }
              return RepeatStatus.FINISHED;
            },
            txManager)
        .build();
  }

  private Job failFastJob(JobDefinition def, Step extract, List<Step> publish) {
    SimpleJobBuilder builder =
        new JobBuilder(def.name(), jobRepository).listener(runLoggingListener).start(extract);
    for (Step step : publish) {
      builder.next(step);
    }
    return builder.build();
  }

  private Job continueJob(JobDefinition def, Step extract, List<Step> publish) {
    Step gate = failureGate(publish.stream().map(Step::getName).toList());
    FlowBuilder<Flow> flow = new FlowBuilder<>("eod-" + def.name());
    flow.start(extract).on("FAILED").fail();
    flow.from(extract).on("*").to(publish.get(0));
    for (int i = 0; i < publish.size() - 1; i++) {
      flow.from(publish.get(i)).on("*").to(publish.get(i + 1));
    }
    Step last = publish.get(publish.size() - 1);
    flow.from(last).on("*").to(gate);
    flow.from(gate).on("FAILED").fail();
    flow.from(gate).on("*").end();
    return new JobBuilder(def.name(), jobRepository)
        .listener(runLoggingListener)
        .start(flow.build())
        .end()
        .build();
  }
}
