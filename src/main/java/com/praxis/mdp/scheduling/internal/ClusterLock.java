package com.praxis.mdp.scheduling.internal;

/**
 * Extension point for a cluster lock (ShedLock or a database lease) when more than one node
 * schedules the same jobs. The default implementation always acquires: this deployment is a single
 * VM.
 */
interface ClusterLock {

  boolean tryAcquire(String jobName);
}
