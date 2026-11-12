package com.praxis.mdp.publishing;

/** One destination a job publishes to. Built in Java from a typed connection. */
public interface Publication {

  /** Step suffix: the batch step is {@code publish-<stepName>}. */
  String stepName();

  Format format();

  /** False when the connection profile is disabled. Disabled profiles are not contacted. */
  boolean active();

  SinkWriter open(PublishRequest request, PublishingClients clients);
}
