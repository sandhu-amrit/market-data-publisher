package com.praxis.mdp.scheduling.internal;

import org.springframework.stereotype.Component;

@Component
class NoOpClusterLock implements ClusterLock {

  @Override
  public boolean tryAcquire(String jobName) {
    return true;
  }
}
