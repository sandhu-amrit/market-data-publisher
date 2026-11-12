package com.praxis.mdp.pipeline;

/** Resolved batch tuning for one job: defaults overlaid with {@code mdp.jobs.<name>.batch}. */
public record BatchSettings(
    int chunkSize, int threads, int skipLimit, int retryLimit, int fetchSize) {

  public BatchSettings {
    require(chunkSize >= 1, "chunkSize", chunkSize);
    require(threads >= 1, "threads", threads);
    require(skipLimit >= 0, "skipLimit", skipLimit);
    require(retryLimit >= 0, "retryLimit", retryLimit);
    require(fetchSize >= 1, "fetchSize", fetchSize);
  }

  private static void require(boolean ok, String field, int value) {
    if (!ok) {
      throw new IllegalArgumentException("batch." + field + " is invalid: " + value);
    }
  }
}
