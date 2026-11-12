package com.praxis.mdp.pipeline;

import java.util.List;
import java.util.Optional;
import org.springframework.batch.core.Job;

/** Java-defined jobs. The bean name of each {@link Job} is {@link JobDefinition#name()}. */
public interface JobCatalog {

  List<JobDefinition> all();

  Optional<JobDefinition> find(String jobName);

  Job requireJob(String jobName);
}
