package com.praxis.mdp.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.praxis.mdp.infrastructure.config.JobExecutors;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.pipeline.internal.jobs.JobStartupValidator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class JobSettingsTest {

  @Test
  void jobBatchOverridesDefaults() {
    MdpProperties props = new MdpProperties();
    props.getBatch().getDefaults().setChunkSize(1000);
    props.getBatch().getDefaults().setThreads(1);
    props.getBatch().getDefaults().setSkipLimit(100);
    MdpProperties.JobConfig futures = new MdpProperties.JobConfig();
    futures.getBatch().setChunkSize(2000);
    futures.getBatch().setThreads(4);
    props.getJobs().put("fxFuturesEodJob", futures);
    props.getJobs().put("fxForwardsEodJob", new MdpProperties.JobConfig());

    JobRuntime runtime = new JobRuntime(props);

    assertThat(runtime.batch("fxFuturesEodJob"))
        .isEqualTo(new BatchSettings(2000, 4, 100, 0, 1000));
    assertThat(runtime.batch("fxForwardsEodJob").chunkSize()).isEqualTo(1000);
    assertThat(runtime.batch("fxForwardsEodJob").threads()).isEqualTo(1);
  }

  @Test
  void noneAndBlankSchedulesAreOnDemand() {
    MdpProperties props = new MdpProperties();
    MdpProperties.JobConfig cme = new MdpProperties.JobConfig();
    cme.setSchedule("none");
    cme.setEnabled(true);
    props.getJobs().put("cmeFuturesEodJob", cme);
    JobRuntime runtime = new JobRuntime(props);
    assertThat(runtime.schedule("cmeFuturesEodJob").scheduled()).isFalse();
    assertThat(runtime.schedule("missing").scheduled()).isFalse();
  }

  @Test
  void invalidChunkSizeNamesTheJob() {
    MdpProperties props = new MdpProperties();
    MdpProperties.JobConfig job = new MdpProperties.JobConfig();
    job.getBatch().setChunkSize(0);
    props.getJobs().put("fxFuturesEodJob", job);
    JobRuntime runtime = new JobRuntime(props);
    assertThatThrownBy(() -> runtime.batch("fxFuturesEodJob"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("fxFuturesEodJob")
        .hasMessageContaining("chunkSize");
  }

  @Test
  void eachJobGetsItsOwnPoolSizedFromThreads() {
    JobExecutors executors = new JobExecutors();
    ThreadPoolTaskExecutor futures =
        (ThreadPoolTaskExecutor) executors.forJob("fxFuturesEodJob", 4);
    ThreadPoolTaskExecutor cme = (ThreadPoolTaskExecutor) executors.forJob("cmeFuturesEodJob", 3);
    assertThat(futures).isNotSameAs(cme);
    assertThat(futures.getCorePoolSize()).isEqualTo(4);
    assertThat(futures.getThreadNamePrefix()).isEqualTo("mdp-fxFuturesEodJob-");
    assertThat(cme.getCorePoolSize()).isEqualTo(3);
    assertThat(cme.getThreadNamePrefix()).isEqualTo("mdp-cmeFuturesEodJob-");
    assertThat(executors.forJob("fxFuturesEodJob", 4)).isSameAs(futures);
    executors.shutdown();
  }

  @Test
  void unknownJobKeyIsRejected() {
    MdpProperties props = new MdpProperties();
    props.getJobs().put("notAJob", new MdpProperties.JobConfig());
    JobCatalog catalog =
        new JobCatalog() {
          @Override
          public List<JobDefinition> all() {
            return List.of();
          }

          @Override
          public java.util.Optional<JobDefinition> find(String jobName) {
            return java.util.Optional.empty();
          }

          @Override
          public org.springframework.batch.core.Job requireJob(String jobName) {
            throw new UnsupportedOperationException();
          }
        };
    JobStartupValidator validator =
        new JobStartupValidator(props, catalog, new JobRuntime(props), new JobExecutors());
    assertThatThrownBy(validator::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("mdp.jobs.notAJob")
        .hasMessageContaining("JobDefinition");
  }
}
