# ADR 0005: Type-safe job wiring; YAML for environment values and tuning only

## Status

Accepted

## Context

Jobs were selected by string names (`destinations: [sftp, kafka]`) and shared one chunk size, one skip limit, and one set of hostnames. A typo in a profile name showed up only when the job ran. Batch threads were one shared pool, so a long FX futures run could occupy every worker a forwards run needed.

Connection details and batch tuning do change between desks and environments. The set of destinations, the file name, the topic, and what happens on a missing price do not: those are part of the job.

## Decision

`JobDefinition` beans wire a typed source and typed publications in Java. `Connections.sftp().vendorA()` is a field, so a profile that does not exist does not compile. YAML holds:

* `mdp.connections.*` (hosts, ports, credentials via `${ENV}`, TLS material, and an `enabled` flag)
* `mdp.batch.defaults` and `mdp.jobs.<job>.batch` (chunk size, threads, skip limit, retry limit, fetch size)
* `mdp.jobs.<job>.schedule` (`none` or absent means on-demand) and `enabled`

Startup fails if any `mdp.jobs` key is not a `JobDefinition`, if a cron does not parse, or if merged tuning is out of range. Each job gets a bounded pool named `mdp-<job>-N` from its resolved thread count. Clients, SFTP session factories, and Kafka producers are created per connection on first use and cached; a disabled profile is never contacted.

There is no `@RefreshScope` and the run API cannot override configuration. Changing a connection or a chunk size means a restart. Vault (or any other secret store) is expected to inject the `${ENV}` placeholders; `spring.config.import: vault://` via Spring Cloud Vault is a deployment choice, not a library this service depends on.

## Consequences

Adding a destination or a policy is a code change and a review. Adding a host, a password, or a larger chunk size is a configuration change and a restart. Two jobs can run at the same time without sharing a thread pool. A publication whose profile is `enabled: false` is left out of the batch flow, which is how local and test runs avoid opening SFTP, Kafka, and REST.
