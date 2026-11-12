# ADR 0002: Spring Batch metadata only, no custom tables

## Status

Accepted

## Context

Operators need to know whether a destination published, who triggered a run, and whether a date is already running. A custom run table would duplicate that and would have to stay in sync with Batch's own executions.

## Decision

The only tables are Spring Batch's metadata tables (H2 locally, Oracle in prod). Per-destination status is one step per destination (`extract`, `publish-file`, `publish-sftp`, `publish-kafka`, `publish-rest`, and `failureGate` when the job's failure policy is `CONTINUE`). Audit fields `triggeredBy` and `triggerSource` are non-identifying job parameters. The run identity is the identifying pair `businessDate` + `runSequence`. A duplicate active run for the same job and date is rejected by reading `JobExplorer.findRunningJobExecutions`. An overwrite rerun allocates the next `runSequence` and writes the same destination paths again.

Startup recovery marks executions left in `STARTED` as `FAILED` so they can be restarted. That update is done through `JobRepository`, not a side table.

## Consequences

Listing runs scans recent job instances (capped) rather than a custom index. Schema changes are Spring Batch's, applied once by a DBA from `schema-oracle.sql` (see `docs/batch-schema.md`). We do not add Flyway for application tables because there are none.
