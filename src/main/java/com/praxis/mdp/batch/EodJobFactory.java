package com.praxis.mdp.batch;

import com.praxis.mdp.config.MdpProperties;
import com.praxis.mdp.model.InstrumentType;
import com.praxis.mdp.model.MarketDataRecord;
import com.praxis.mdp.sink.MarketDataSink;
import com.praxis.mdp.source.MarketDataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Builds a standard end-of-day job for one instrument type: read snapshot -> validate -> publish.
 * Adding a job for another instrument type is one more call to {@link #create} in {@code JobsConfig}.
 * When more destinations arrive, add further steps here (one per destination).
 */
@Component
public class EodJobFactory {

    public static final String PUBLISH_FILE_STEP = "publishFile";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final MarketDataSource source;
    private final MarketDataSink fileSink;
    private final RunLoggingListener runLogging;
    private final MdpProperties props;

    public EodJobFactory(JobRepository jobRepository, PlatformTransactionManager txManager,
                         MarketDataSource source, MarketDataSink fileSink,
                         RunLoggingListener runLogging, MdpProperties props) {
        this.jobRepository = jobRepository;
        this.txManager = txManager;
        this.source = source;
        this.fileSink = fileSink;
        this.runLogging = runLogging;
        this.props = props;
    }

    public Job create(String jobName, InstrumentType type) {
        return new JobBuilder(jobName, jobRepository)
                .listener(runLogging)
                .start(publishFileStep(type))
                .build();
    }

    private Step publishFileStep(InstrumentType type) {
        return new StepBuilder(PUBLISH_FILE_STEP, jobRepository)
                .<MarketDataRecord, MarketDataRecord>chunk(props.chunkSize(), txManager)
                .reader(new LazyReader(source, type))
                .processor(new MarketDataValidator(type))
                .writer(new LazySinkWriter(fileSink, type))
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(InvalidRecordException.class)
                .skipLimit(props.skipLimit())
                .listener(new SkipLoggingListener())
                .build();
    }
}
