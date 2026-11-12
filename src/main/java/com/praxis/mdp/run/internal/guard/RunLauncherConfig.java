package com.praxis.mdp.run.internal.guard;

import com.praxis.mdp.infrastructure.config.JobExecutors;
import com.praxis.mdp.pipeline.JobRuntime;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;

@Configuration
public class RunLauncherConfig {

  @Bean
  @Primary
  TaskExecutorJobLauncher asyncJobLauncher(
      JobRepository jobRepository, JobExecutors executors, JobRuntime runtime) throws Exception {
    PerJobJobLauncher launcher = new PerJobJobLauncher(jobRepository, executors, runtime);
    launcher.setJobRepository(jobRepository);
    launcher.setTaskExecutor(new SyncTaskExecutor());
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
