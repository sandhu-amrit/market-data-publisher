package com.praxis.mdp.pipeline.internal.jobs;

import com.praxis.mdp.infrastructure.config.JobExecutors;
import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.pipeline.JobCatalog;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.JobRuntime;
import com.praxis.mdp.pipeline.ScheduleSettings;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * Fails startup when YAML names an unknown job, a cron is invalid, or batch tuning does not merge.
 */
@Component
public class JobStartupValidator implements SmartInitializingSingleton {

  private final MdpProperties props;
  private final JobCatalog catalog;
  private final JobRuntime runtime;
  private final JobExecutors executors;

  public JobStartupValidator(
      MdpProperties props, JobCatalog catalog, JobRuntime runtime, JobExecutors executors) {
    this.props = props;
    this.catalog = catalog;
    this.runtime = runtime;
    this.executors = executors;
  }

  @Override
  public void afterSingletonsInstantiated() {
    validate();
  }

  public void validate() {
    var defined =
        catalog.all().stream()
            .map(JobDefinition::name)
            .collect(Collectors.toCollection(TreeSet::new));
    for (String key : props.getJobs().keySet()) {
      if (!defined.contains(key)) {
        throw new IllegalStateException(
            "mdp.jobs." + key + " does not match a JobDefinition. Defined jobs: " + defined);
      }
    }
    for (JobDefinition job : catalog.all()) {
      runtime.batch(job.name());
      ScheduleSettings schedule = runtime.schedule(job.name());
      if (schedule.cron() != null) {
        try {
          new CronTrigger(schedule.cron());
        } catch (IllegalArgumentException e) {
          throw new IllegalStateException(
              "Job " + job.name() + " has an invalid schedule '" + schedule.cron() + "'", e);
        }
      }
      executors.forJob(job.name(), runtime.batch(job.name()).threads());
    }
  }
}
