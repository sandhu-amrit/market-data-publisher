# ADR 0001: Spring Modulith packages instead of Maven modules

## Status

Accepted

## Context

The publisher is one deployable service: one process, one database, one release. Splitting it into Maven modules would add a reactor, version alignment, and a second build graph without a second deployment unit. The boundaries that matter are the ones inside the process: who may call the run API, who may see a sink's client, and the direction source → pipeline → destinations.

## Decision

Top-level packages under `com.praxis.mdp` are Spring Modulith modules. Public types live in the module root. Everything under `internal` is hidden. `domain` and `infrastructure` are open modules (`ApplicationModule.Type.OPEN`) because they are the shared kernel and the shared technical configuration. `ApplicationModules.verify()` runs in the build and rejects a dependency that points the wrong way, including anything that depends on `run` or `scheduling`.

Scheduling starts a run through `ScheduledRunLauncher` on the pipeline module. The run module implements that port, so the scheduler does not import the run module.

## Consequences

A change that reaches into another module's `internal` package fails `./mvnw verify`. We do not get per-module classloaders or independent versioning. If a module later needs its own release cadence, it can be extracted; the package boundary is the seam.
