package com.praxis.mdp.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchInfrastructureConfig {

  @Bean
  @ConditionalOnMissingBean
  Clock mdpClock(MdpProperties props) {
    if (props.getScheduleZone() == null || props.getScheduleZone().isBlank()) {
      return Clock.systemDefaultZone();
    }
    return Clock.system(ZoneId.of(props.getScheduleZone()));
  }

  @Bean(name = "mdpJobExecutor")
  TaskExecutor mdpJobExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("mdp-run-");
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(20);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    return executor;
  }

  @Bean
  @Primary
  TaskExecutorJobLauncher asyncJobLauncher(JobRepository jobRepository, TaskExecutor mdpJobExecutor)
      throws Exception {
    TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
    launcher.setJobRepository(jobRepository);
    launcher.setTaskExecutor(mdpJobExecutor);
    launcher.afterPropertiesSet();
    return launcher;
  }

  @Bean
  @Primary
  JobOperator mdpJobOperator(
      TaskExecutorJobLauncher asyncJobLauncher,
      JobRepository jobRepository,
      JobExplorer jobExplorer,
      JobRegistry jobRegistry)
      throws Exception {
    SimpleJobOperator operator = new SimpleJobOperator();
    operator.setJobLauncher(asyncJobLauncher);
    operator.setJobRepository(jobRepository);
    operator.setJobExplorer(jobExplorer);
    operator.setJobRegistry(jobRegistry);
    operator.afterPropertiesSet();
    return operator;
  }
}
