package com.praxis.mdp.pipeline.internal.jobs;

import com.praxis.mdp.pipeline.JobCatalog;
import com.praxis.mdp.pipeline.JobDefinition;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.batch.core.Job;
import org.springframework.stereotype.Component;

@Component
public class DefaultJobCatalog implements JobCatalog {

  private final List<JobDefinition> definitions;
  private final Map<String, Job> jobs;

  public DefaultJobCatalog(List<JobDefinition> definitions, List<Job> jobs) {
    this.definitions =
        definitions.stream().sorted(Comparator.comparing(JobDefinition::name)).toList();
    Map<String, Job> byName = new HashMap<>();
    for (Job job : jobs) {
      byName.put(job.getName(), job);
    }
    this.jobs = Map.copyOf(byName);
  }

  @Override
  public List<JobDefinition> all() {
    return definitions;
  }

  @Override
  public Optional<JobDefinition> find(String jobName) {
    return definitions.stream().filter(def -> def.name().equals(jobName)).findFirst();
  }

  @Override
  public Job requireJob(String jobName) {
    Job job = jobs.get(jobName);
    if (job == null) {
      throw new IllegalArgumentException("No Job bean named " + jobName);
    }
    return job;
  }
}
