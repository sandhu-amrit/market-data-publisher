# Rebuild guide

This guide is for a coding agent that has `bundle/ALL_CODE.md` and must recreate the Market Data Publisher tree byte for byte, then keep later changes inside the module rules below.

The archive was taken from commit `3fadf78c7185abbbc9b1e7f070dede9fec7beed2` on `cursor/modulith-restructure-17e8`. It contains **147 files**. The table of contents in `ALL_CODE.md` is the complete list, in this order: build files, the Spring Boot application class, main code by module (`domain`, `source`, `pipeline`, `publishing`, `run`, `scheduling`, `infrastructure`), resources and sample data, tests, operations, documentation.

`bundle/verify_bundle.py` is the reference extractor. It is not inside `ALL_CODE.md`.

## What the archive includes

Every tracked text file of the project: Java main and test sources (including every `package-info.java`), `pom.xml`, Maven wrapper scripts and `maven-wrapper.properties`, Checkstyle XML, GitHub Actions, Docker and systemd files, sample CSVs, README, ADRs, and `docs/batch-schema.md`.

These items are absent because they are absent on the branch. Do not invent them.

- No `.mvn/wrapper/maven-wrapper.jar`. `distributionType=only-script` and `wrapperVersion=3.3.4`. `./mvnw` downloads Maven **3.9.9** from the `distributionUrl` in `.mvn/wrapper/maven-wrapper.properties`. Regenerate the scripts, when you must, with `mvn wrapper:wrapper -Dmaven=3.9.9 -Dtype=only-script`. Leave the jar out of the tree. After extraction, `mvnw` is mode `0755`. `mvnw.cmd` is CRLF; keep those bytes.
- No Logback XML and no `logback-spring.xml`. Structured console logging is the single setting `logging.structured.format.console=ecs` in `src/main/resources/application.yml`.
- No `.sql` file and no Flyway migration. Run history is Spring Batch metadata only. Oracle DDL is `org/springframework/batch/core/schema-oracle.sql` inside `spring-batch-core`. The copy steps are in `docs/batch-schema.md`. The document names Spring Batch **5.2.6** as the line managed by Spring Boot **3.5.16**.
- No standalone Spotless file. Google Java Format **1.25.2**, style `GOOGLE`, plus `removeUnusedImports`, `trimTrailingWhitespace`, and `endWithNewline`, lives in the `spotless-maven-plugin` block in `pom.xml` and runs at `verify`. Checkstyle rules are `config/checkstyle/checkstyle.xml` (newline at end of file, no tabs, unused imports, no star imports, braces, one top-level type). The plugin includes test sources and fails the build on violations.

## Target directory tree

147 tracked paths. Dotfiles sort after letters in this drawing; the archive order is the table of contents, not this drawing.

```text
.
├── .github
│   └── workflows
│       └── ci.yml
├── .mvn
│   └── wrapper
│       └── maven-wrapper.properties
├── config
│   └── checkstyle
│       └── checkstyle.xml
├── deploy
│   ├── k8s
│   │   └── README.md
│   ├── docker-compose.yml
│   ├── Dockerfile
│   └── mdp.service
├── docs
│   ├── adr
│   │   ├── 0001-modulith-over-maven-modules.md
│   │   ├── 0002-batch-metadata-only.md
│   │   ├── 0003-jobs-in-java.md
│   │   ├── 0004-api-triggered-scheduling.md
│   │   └── 0005-type-safe-job-wiring.md
│   └── batch-schema.md
├── sample-data
│   └── input
│       ├── CME_FUTURE_2026-11-11.csv
│       ├── FX_FORWARD_2026-11-11.csv
│       └── FX_FUTURE_2026-11-11.csv
├── src
│   ├── main
│   │   ├── java/com/praxis/mdp/
│   │   │   ├── MarketDataPublisherApplication.java
│   │   │   ├── domain/                 (open shared module)
│   │   │   ├── source/
│   │   │   ├── pipeline/
│   │   │   ├── publishing/
│   │   │   ├── run/
│   │   │   ├── scheduling/
│   │   │   └── infrastructure/         (open shared module)
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-dev.yml
│   │       ├── application-oracle.yml
│   │       └── application-prod.yml
│   └── test
│       ├── java/com/praxis/mdp/        (mirrors the packages under test)
│       └── resources/
│           ├── application.properties
│           ├── application-test.yml
│           └── input/FX_FUTURE_2026-11-11.csv
├── .dockerignore
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

The full file names under each Java package are the matching entries in the `ALL_CODE.md` table of contents. Recreate those paths exactly. `bundle/` itself is not part of the archive.

Coordinates that the tree above collapses:

| Path | Role |
|---|---|
| `src/main/java/com/praxis/mdp/MarketDataPublisherApplication.java` | Boot and Modulith entry. Package `com.praxis.mdp`. |
| `src/main/java/com/praxis/mdp/<module>/` | Public types of that module, plus `package-info.java`. |
| `src/main/java/com/praxis/mdp/<module>/internal/...` | Hidden implementation. |
| `src/main/resources/` | `application*.yml` and `application.properties` only. |
| `sample-data/input/` | Workstation CSVs named `<INSTRUMENT_TYPE>_<yyyy-MM-dd>.csv`. |
| `src/test/java/` | Tests, same package as the code they call, including `internal`. |
| `src/test/resources/` | Test profile and the FX future fixture. |
| `config/checkstyle/checkstyle.xml` | Checkstyle. |
| `deploy/` | Dockerfile, Compose, systemd unit, Kubernetes notes. |
| `.github/workflows/ci.yml` | `./mvnw -B verify` on Temurin 17. |
| `docs/adr/` and `docs/batch-schema.md` | Decisions and Oracle Batch DDL notes. |
| `README.md`, `.gitignore`, `.dockerignore` | Operator docs and ignore rules. `.dockerignore` ignores `*.md`, `target`, `output`, `work`, and `.git`. |

Maven coordinates in `pom.xml`: `com.praxis:market-data-publisher:0.2.0-SNAPSHOT`, parent Spring Boot **3.5.16**, Java **17**, Spring Modulith **1.4.14**.

## Module map

Top-level packages under `com.praxis.mdp` are the Spring Modulith modules. `MarketDataPublisherApplication` is annotated `@Modulithic(systemName = "Market Data Publisher", sharedModules = {"domain", "infrastructure"})` and `@SpringBootApplication(exclude = KafkaAutoConfiguration.class)`. It scans configuration properties. `spring.batch.job.enabled=false` in both `application.yml` and `application.properties`, so Boot does not launch jobs on startup.

`domain` and `infrastructure` are `ApplicationModule.Type.OPEN` and are the shared modules. An open module's types, including types outside the module root, are visible to other modules. `domain` has `allowedDependencies = {}`. `infrastructure` may depend only on `domain`.

| Module | `package-info` allowed dependencies | Public API (module root, or any package because the module is open) | `internal` |
|---|---|---|---|
| `domain` (open) | none | Records `Instrument`, `EodPrice`, `MarketDataRecord`, `BusinessDate`, `RunId`. Enum `InstrumentType` (`FX_FUTURE`, `FX_FORWARD`, `CME_FUTURE`). Subpackage `domain.policy`: enums `FailurePolicy`, `MissingPricePolicy`. There is no `internal` package. | none |
| `source` | `domain`, `infrastructure` | `MarketDataSource` (reader SPI). `AssetControlSource extends MarketDataSource` (the vendor-shaped source the jobs inject). | `internal.file.FileMarketDataSource` (the `@Component` that implements `AssetControlSource` and reads CSV). `internal.assetcontrol.AssetControlMarketDataSource` is a final class with a private constructor, `STATUS = "NOT_IMPLEMENTED"`, and is not a bean and not a `MarketDataSource`. |
| `pipeline` | `domain`, `infrastructure`, `source`, `publishing` | `JobDefinition`, `JobCatalog`, `JobRuntime`, `ScheduledRunLauncher`, `RunParameters`, `BatchSettings`, `ScheduleSettings`. | `internal.jobs` (`FxFuturesJob`, `FxForwardsJob`, `CmeFuturesJob`, `DefaultJobCatalog`, `JobStartupValidator`). `internal.flow` (`EodJobFactory` and the extract/publish reader and writer). `internal.extract.SnapshotStore`. `internal.validate`. `internal.transform` (`Transformer`, `Serializer`, `CsvSerializer`, `IdentityTransformer`). `internal.listener` (MDC, skip metrics, step metrics, startup recovery). |
| `publishing` | `domain`, `infrastructure` | `Publication`, `MarketDataSink`, `SinkWriter`, `PublishingClients`, `PublishRequest`, `Format` (`CSV`, `JSON`), `OutputNames`, `FileDestination`, `SftpDestination`, `KafkaDestination`, `RestDestination`. | `internal.DefaultPublishingClients`. `internal.file`, `internal.sftp`, `internal.kafka`, `internal.rest`. |
| `run` | `domain`, `infrastructure`, `pipeline` | `RunService` (implements `ScheduledRunLauncher`), `RunStatus`. | `internal.api` (`RunController` at `/api/v1/runs`, `RunRequest`, `ApiExceptionHandler`, `OpenApiConfig`). `internal.guard` (`ActiveRunGuard`, `PerJobJobLauncher`, `RunLauncherConfig`, `RunExceptions`). `internal.cli.CliRunner`. |
| `scheduling` | `domain`, `infrastructure`, `pipeline` | No type in the module root. The module root holds only `package-info.java`. | `JobScheduler`, package-private `ClusterLock`, package-private `NoOpClusterLock`. |
| `infrastructure` (open) | `domain` | `config.MdpProperties`, `config.Connections`, `config.JobExecutors`, `config.BatchInfrastructureConfig` (the `Clock`). `security.SecurityConfig`, `security.JwtRolesConverter`. `observability.RunMdc`, `observability.PublishMetrics`, `observability.WorkDirectoryHealthIndicator`, `observability.BatchMetadataHealthIndicator`. | No `internal` package. The module is open, so `config`, `security`, and `observability` are shared. |

Named interfaces other modules are meant to program against:

- `source.MarketDataSource`, `source.AssetControlSource`
- `pipeline.JobCatalog`, `pipeline.ScheduledRunLauncher`
- `publishing.Publication`, `publishing.MarketDataSink`, `publishing.SinkWriter`, `publishing.PublishingClients`
- `pipeline.internal.transform.Transformer` and `Serializer` stay inside `pipeline`
- `scheduling.internal.ClusterLock` is package-private

`PublishingClients` is public and its accessors return `SftpSessions`, `KafkaProducers`, and `RestClients`, which live under `publishing.internal`. Leave that shape as archived. Other modules reach a destination through `Publication`. `ApplicationModules.verify()` rejects a module that imports a package outside its `allowedDependencies`.

`scheduling` starts work by calling `ScheduledRunLauncher`. `RunService` is the implementation. The scheduler source does not import `com.praxis.mdp.run`.

## Allowed dependency directions

Enforced by `@ApplicationModule(allowedDependencies = ...)` and by `ModularityTests.verifyModularStructure`, which calls `ApplicationModules.of(MarketDataPublisherApplication.class).verify()`.

```text
scheduling ──► pipeline ──► source
     │              │
     │              └──► publishing
     │
run ─────────► pipeline

domain ◄── every module that needs the kernel
infrastructure ◄── every module except domain
infrastructure ──► domain

domain depends on nothing.
source does not depend on pipeline.
publishing does not depend on pipeline.
pipeline does not depend on run or scheduling.
nothing depends on run.
nothing depends on scheduling.
```

Shared modules: `domain` and `infrastructure` (`sharedModules` on `@Modulithic`, and `Type.OPEN` on their `package-info.java`).

## Package naming

- Root package: `com.praxis.mdp`.
- One module = one direct subpackage: `com.praxis.mdp.<module>`.
- Public types of a closed module sit in that module package. `package-info.java` carries `@ApplicationModule`.
- Hidden code sits under `com.praxis.mdp.<module>.internal` and further subpackages (`jobs`, `flow`, `file`, `sftp`, `kafka`, `rest`, `api`, `guard`, `cli`, `assetcontrol`).
- `domain.policy` is a public subpackage of the open domain module. It is not named `internal`.
- `infrastructure.config`, `infrastructure.security`, and `infrastructure.observability` are the open infrastructure packages.
- A new closed-module type that other modules call belongs in the module root. A new adapter belongs under `internal`.
- Tests use the same package as the production type so they can construct package-visible types (`ClusterLock` is the exception: it is package-private and has no direct test).

## Where resources, tests, and ops files go

| Kind | Path |
|---|---|
| Default profile and safe workstation defaults | `src/main/resources/application.yml` (`spring.profiles.default=local`) |
| Profile overlays | `application-local.yml`, `application-dev.yml`, `application-oracle.yml`, `application-prod.yml` |
| Batch auto-start off | `src/main/resources/application.properties` (`spring.batch.job.enabled=false`) |
| Sample input | `sample-data/input/<TYPE>_<yyyy-MM-dd>.csv` with header `instrumentId,instrumentType,symbol,currency,maturityDate,closePrice` |
| Test profile activation | `src/test/resources/application.properties` sets `spring.profiles.active=test` |
| Test overlays | `src/test/resources/application-test.yml` (output `target/test-output`, work `target/test-work`, input `src/test/resources/input`, all three job schedules `enabled: false`) |
| Test fixture | `src/test/resources/input/FX_FUTURE_2026-11-11.csv` |
| Build | `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `config/checkstyle/checkstyle.xml` |
| CI | `.github/workflows/ci.yml` |
| Image and local dependencies | `deploy/Dockerfile`, `deploy/docker-compose.yml`, `deploy/mdp.service`, `deploy/k8s/README.md`, `.dockerignore` |
| Docs | `README.md`, `docs/adr/0001` through `0005`, `docs/batch-schema.md` |

Runtime directories `output/`, `work/`, and `target/` are gitignored. Do not archive them.

## Extraction procedure

Work from the bytes. Do not pretty-print, re-encode, or change newlines. `mvnw.cmd` contains CRLF; every file ends with a newline.

1. Read `bundle/ALL_CODE.md` as bytes.
2. Find each header line that matches `^===== FILE: <relative-path> =====$`. The path is the exact repo-relative path.
3. The file bytes start on the next byte and run up to, and not including, the line `===== END FILE =====`. Those bytes include the file's own trailing newline, which is the newline immediately before the closer.
4. Reject a path that is absolute or contains a `..` segment. Reject a duplicate path.
5. Write each file under the target root, creating parent directories. Set `mvnw` executable (`0755`).
6. Stop when every table-of-contents path exists and `git status` would be empty against this archive (147 files, byte-identical).

`python3 bundle/verify_bundle.py` does steps 1–5 into a temp directory, compares each file with the checkout that contains `bundle/`, then runs `./mvnw -B verify` in the temp directory. Use that script instead of a second parser.

A minimal extractor, equivalent to the script:

```python
import re
from pathlib import Path

archive = Path("bundle/ALL_CODE.md").read_bytes()
start = re.compile(br"(?m)^===== FILE: ([^\r\n]+) =====\n")
for match in start.finditer(archive):
    rel = match.group(1).decode()
    end = archive.find(b"===== END FILE =====", match.end())
    content = archive[match.end():end]
    path = Path("rebuilt") / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
```

Then `chmod 0755 rebuilt/mvnw`.

## Validation

From the rebuilt root, with JDK 17 or a newer JDK that can compile `--release 17`:

```bash
chmod 0755 mvnw
./mvnw -B verify
./mvnw -B test -Dtest=ModularityTests
```

`./mvnw verify` is what CI runs (`.github/workflows/ci.yml`, Temurin 17). It runs tests, Spotless `check`, Checkstyle, and the JaCoCo report (`target/site/jacoco`). The pom sets no coverage threshold.

`ModularityTests` has two tests:

- `verifyModularStructure` calls `ApplicationModules.verify()` and asserts `domain` and `infrastructure` are open, `domain` has no direct dependencies, and `scheduling`, `pipeline`, `source`, and `publishing` do not depend on `run` (and `source` / `publishing` do not depend on `pipeline`).
- `writeModuleDocumentation` writes Modulith docs under `target/spring-modulith-docs`.

Test count on this branch: **37** `@Test` methods in **19** classes. Two of them are gated with `@EnabledIf("dockerAvailable")`: `KafkaMarketDataSinkTest` and `SftpMarketDataSinkTest`. Without Docker, Surefire reports `Tests run: 37, Failures: 0, Errors: 0, Skipped: 2` (35 executed, the two container tests skipped). With Docker, the same 37 methods run and skipped is 0, provided the images can be pulled. `bundle/verify_bundle.py` prints those totals from `target/surefire-reports`.

## Rules for adding a job, source, sink, or connection profile

Follow the archived shape. YAML holds environment values and tuning. Java holds the job shape.

### Job

Add a `@Configuration` in `pipeline.internal.jobs`, next to `FxFuturesJob`.

- Expose `public static final String NAME`.
- A `JobDefinition` `@Bean` built with `JobDefinition.builder(NAME)`, `.instrumentType(...)`, `.source(...)`, `.missingPrices(...)`, `.onFailure(...)`, and one or more `.publishTo(...)`.
- A Spring Batch `Job` `@Bean` that returns `EodJobFactory.create(definition)`. The Batch job name is `JobDefinition.name()`. The `@Bean` method for the `Job` in the existing classes is named `<family>EodBatchJob`, so the Spring bean name and the Batch job name differ. `DefaultJobCatalog` indexes `Job.getName()`, which comes from `JobBuilder(def.name())`.
- A new instrument family needs a new `InstrumentType` constant and a sample file `sample-data/input/<TYPE>_<yyyy-MM-dd>.csv`.
- Add `mdp.jobs.<NAME>` in `application.yml` with `enabled`, `schedule` (Spring 6-field cron, or `none`), and optional `batch` overrides. `JobStartupValidator` fails startup when an `mdp.jobs` key is not a `JobDefinition`, when the cron does not parse, or when merged tuning is out of range. It also creates that job's executor.
- In `src/test/resources/application-test.yml`, set `mdp.jobs.<NAME>.enabled: false` so weekday crons do not fire during tests.

`MdpProperties.JobConfig` has `schedule`, `enabled`, and `batch` only. `JobRuntime` reads those. Policies and the publication list stay on the `JobDefinition`. ADR 0003 mentions configuration overriding the two policies and a destination filter; those fields are not on `MdpProperties`. Recreate the Java that is archived.

### Source

- Add the SPI method on `MarketDataSource` only when every source must grow it. Otherwise implement `MarketDataSource` or `AssetControlSource`.
- Put the adapter in `source.internal.<vendor>`. Register it as a bean only when a job should receive it.
- Jobs in `pipeline.internal.jobs` inject `AssetControlSource` today. `FileMarketDataSource` is that bean. `AssetControlMarketDataSource` stays a non-bean placeholder until the vendor client exists. The placeholder comment says to page instrument ids, store the resume cursor in the step `ExecutionContext`, and select it with `mdp.source.type=asset-control`. That property is not implemented yet. Do not add it unless you are doing that TODO.
- A source module class may use `domain` and `infrastructure`. It may not import `pipeline`, `publishing`, `run`, or `scheduling`.

### Sink

- Add a public builder in the `publishing` root, the way `SftpDestination` does: `to(typedConnection)`, `stepName()`, `format()`, `active()`, and `open(PublishRequest, PublishingClients)`.
- `EodJobFactory` names the step `publish-` + `publication.stepName()` and drops publications whose `active()` is false. `FileDestination.active()` is always true. Remote destinations are active when `connection.enabled()` is true. A disabled profile is omitted from the flow and is not contacted.
- Put the client and the `MarketDataSink` under `publishing.internal.<kind>`.
- File and SFTP write a sibling `.tmp` and then move or rename onto the final name. SFTP removes an existing remote final path before the rename. Kafka records are keyed by instrument id on an idempotent producer (`acks=all`). REST posts each chunk as JSON with header `Idempotency-Key` equal to `job:businessDate:runSequence:chunkIndex`, with Resilience4j retry and a circuit breaker taken from `RestConnection`.
- `OutputNames.apply` replaces `{job}`, `{type}`, `{businessDate}`, `{date}`, and `{runSequence}`.
- A sink may use `domain` and `infrastructure`. It may not import `pipeline`.

### Connection profile

- Add a component on the `Connections` record: a new field such as `vendorB` on `SftpConnections`, or a new nested record if it is a new kind of system. Jobs call `connections.sftp().vendorA()` (and the other accessors). A name that is not a Java field does not compile.
- Mirror it under `mdp.connections` in `application.yml`. Secrets are `${ENV:}` placeholders only. Nothing secret is committed.
- `enabled` defaults to false for SFTP, Kafka, and REST. Clients, session factories, and producers are created on first use and cached (`SftpSessions`, `KafkaProducers`, `RestClients`).
- There is no `@RefreshScope`. The run API cannot override configuration. Changing a host, a secret, or a chunk size means a restart. Vault is expected to inject the environment variables. `spring.config.import: vault://` (Spring Cloud Vault) is a deployment choice; that library is not a dependency in `pom.xml`.

### Batch tuning and schedules

Defaults in `application.yml`: chunk size 1000, threads 1, skip limit 100, retry limit 0, fetch size 1000. Per-job `mdp.jobs.<name>.batch` overrides through `JobRuntime.batch`. `BatchSettings` rejects chunk size, threads, or fetch size below 1, and skip or retry limits below 0.

`threads` sizes the per-job `ThreadPoolTaskExecutor` in `JobExecutors` (name prefix `mdp-<job>-`, core = max = threads, queue `max(8, threads * 2)`). `PerJobJobLauncher` runs that job's launches on that pool. `EodJobFactory` does not put a task executor on the step, so one execution processes chunks on a single thread. The pool is how many launches of that job can run at once, and it is not shared with other jobs.

`schedule: none`, blank, or a missing cron means on demand (`ScheduleSettings.scheduled()` is false). `enabled: false` skips cron registration and still allows the API to launch the job. `mdp.schedule-zone` (`MDP_SCHEDULE_ZONE`) is the cron zone and the `Clock` zone; empty uses the JVM zone. The scheduler pool is `mdp-schedule-` with size 2.

### Run identity

Identifying job parameters: `businessDate`, `runSequence` (both `true` in `JobParametersBuilder`). Audit-only: `triggeredBy`, `triggerSource` (`API`, `SCHEDULER`, `CLI`). `ActiveRunGuard` rejects a second run of the same job and date while one is running, and allocates `max(existing sequence for that date) + 1`, scanning up to 5000 instances. A new sequence overwrites the local file when the file name has no sequence token. Restart (`POST /api/v1/runs/{id}/restart`) keeps the same parameters, including `runSequence`, and re-runs failed steps. `StartupRecovery` marks executions still `STARTED` as `FAILED` through `JobRepository` so they can be restarted.
