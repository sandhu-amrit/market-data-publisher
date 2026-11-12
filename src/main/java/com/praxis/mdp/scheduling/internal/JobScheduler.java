package com.praxis.mdp.scheduling.internal;

import com.praxis.mdp.infrastructure.config.MdpProperties;
import com.praxis.mdp.pipeline.JobCatalog;
import com.praxis.mdp.pipeline.JobDefinition;
import com.praxis.mdp.pipeline.JobRuntime;
import com.praxis.mdp.pipeline.ScheduleSettings;
import com.praxis.mdp.pipeline.ScheduledRunLauncher;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

@Configuration
@EnableScheduling
public class JobScheduler implements SchedulingConfigurer {

  private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

  private final JobCatalog catalog;
  private final JobRuntime runtime;
  private final ScheduledRunLauncher launcher;
  private final ClusterLock lock;
  private final Clock clock;
  private final String zone;

  public JobScheduler(
      JobCatalog catalog,
      JobRuntime runtime,
      ScheduledRunLauncher launcher,
      ClusterLock lock,
      Clock clock,
      MdpProperties props) {
    this.catalog = catalog;
    this.runtime = runtime;
    this.launcher = launcher;
    this.lock = lock;
    this.clock = clock;
    this.zone = props.getScheduleZone() == null ? "" : props.getScheduleZone();
  }

  @Bean(destroyMethod = "shutdown")
  ThreadPoolTaskScheduler mdpTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(2);
    scheduler.setThreadNamePrefix("mdp-schedule-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    return scheduler;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    registrar.setScheduler(mdpTaskScheduler());
    for (JobDefinition job : catalog.all()) {
      ScheduleSettings schedule = runtime.schedule(job.name());
      if (!schedule.scheduled()) {
        log.info("Job {} is on-demand (no schedule)", job.name());
        continue;
      }
      CronTrigger trigger =
          zone.isBlank()
              ? new CronTrigger(schedule.cron())
              : new CronTrigger(schedule.cron(), ZoneId.of(zone));
      registrar.addTriggerTask(() -> fire(job), trigger);
      log.info("Scheduled {} with cron '{}'", job.name(), schedule.cron());
    }
  }

  private void fire(JobDefinition job) {
    if (!lock.tryAcquire(job.name())) {
      log.info("Skipping scheduled run of {}; lock not acquired", job.name());
      return;
    }
    LocalDate businessDate = LocalDate.now(clock);
    log.info("Scheduled trigger for {} businessDate={}", job.name(), businessDate);
    launcher.launchScheduled(job.name(), businessDate);
  }
}
