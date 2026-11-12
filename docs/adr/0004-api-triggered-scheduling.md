# ADR 0004: Autosys and Airflow call the run API

## Status

Accepted

## Context

Schedulers already exist outside this service (Autosys today, Airflow where a desk has it). A second scheduler inside the JVM is still useful for the desks that want a cron next to the job, but the enterprise trigger path has to be the same API the GUI uses, with the same duplicate-run guard and the same audit parameters.

## Decision

`POST /api/v1/runs` is the supported way to start a run, including back-dates. Autosys and Airflow obtain a JWT with `MDP_OPERATOR` and call that endpoint. `triggeredBy` is the token subject, not a field in the body. The in-process scheduler (`JobScheduler`) is optional per job (`schedule-enabled`, Spring 6-field cron) and calls the same launch path through `ScheduledRunLauncher`. A CLI fallback sets `mdp.cli.job` and `mdp.cli.business-date`.

The process is a single VM under systemd today. `ClusterLock` is a no-op so a ShedLock (or equivalent) implementation can be dropped in when a second replica appears. Until then, do not run two schedulers against the same Oracle schema without that lock: the duplicate guard is transactional per database but two nodes can still pass it together.

## Consequences

External schedulers own calendars, retries, and alerting. This service owns the run. On-demand jobs (CME futures by default) simply leave `schedule-enabled` false. Kubernetes notes in `deploy/k8s/README.md` keep the same API and the same single-replica assumption until the lock exists.
