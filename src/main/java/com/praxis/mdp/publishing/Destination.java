package com.praxis.mdp.publishing;

/** An enabled publish destination. Disabled destinations are not registered as beans. */
public interface Destination {

  /** Stable id: {@code file}, {@code sftp}, {@code kafka}, {@code rest}. */
  String name();

  /** Lower values run first. */
  int order();

  MarketDataSink sink();
}
