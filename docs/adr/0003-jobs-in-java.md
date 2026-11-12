# ADR 0003: Jobs are defined in Java, not a YAML job DSL

## Status

Accepted

## Context

Each job is a small, fixed shape: one instrument type, one extract, then one step per enabled destination, plus a failure policy and a missing-price policy. A YAML DSL for steps would hide that shape and make the flow transitions (fail-fast versus continue) harder to test.

## Decision

`FxFuturesJob`, `FxForwardsJob`, and `CmeFuturesJob` are `@Configuration` classes. They build a `JobDefinition` (Java defaults) and a Spring Batch `Job` through `EodJobFactory`. Configuration may override cron, whether the schedule is enabled, the two policies, and an optional destination filter. It does not define steps.

Adding an instrument family is a new job class plus a sample file, not a new document format.

## Consequences

The catalog is code-reviewed with the flow. Operators change when a job runs, and what it does when a price is missing or a destination fails, without redeploying a new step graph. A job whose structure really differs from this template needs a new factory method, on purpose.
