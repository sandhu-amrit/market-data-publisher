package com.praxis.mdp.infrastructure.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * One bounded pool per job, named {@code mdp-<job>-N}, so a busy job cannot take another job's
 * threads. Pools are created on first use and are not refreshed.
 */
@Component
public class JobExecutors {

  private final ConcurrentMap<String, ThreadPoolTaskExecutor> pools = new ConcurrentHashMap<>();

  public TaskExecutor forJob(String jobName, int threads) {
    if (threads < 1) {
      throw new IllegalArgumentException("Job " + jobName + " threads must be >= 1");
    }
    return pools.computeIfAbsent(jobName, name -> create(name, threads));
  }

  public ThreadPoolTaskExecutor require(String jobName) {
    ThreadPoolTaskExecutor executor = pools.get(jobName);
    if (executor == null) {
      throw new IllegalStateException("No executor for " + jobName);
    }
    return executor;
  }

  public void shutdown() {
    pools.values().forEach(ThreadPoolTaskExecutor::shutdown);
  }

  private static ThreadPoolTaskExecutor create(String jobName, int threads) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("mdp-" + jobName + "-");
    executor.setCorePoolSize(threads);
    executor.setMaxPoolSize(threads);
    executor.setQueueCapacity(Math.max(8, threads * 2));
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
  }
}
