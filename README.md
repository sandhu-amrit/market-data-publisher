# Market Data Publisher

Extracts end-of-day market data (instrument reference data + EOD prices) for one instrument type and one
business date, validates it, and publishes it downstream. **v1 is deliberately small:**

* the source is a CSV file (stand-in for Asset Control),
* the only destination is a CSV file in an output folder,
* one job: `fxFuturesEodJob`.

Stack: Java 17, Spring Boot 3.5, Spring Batch 5, Maven. Run history lives only in Spring Batch's own
metadata tables (H2 locally, Oracle in production) - no custom tables.

## Build and run

Requires JDK 17+ (Maven wrapper included, or use your own Maven).

```bash
./mvnw verify                       # build + all tests
./mvnw spring-boot:run              # or: java -jar target/market-data-publisher-0.1.0-SNAPSHOT.jar
```

The app listens on `http://localhost:8080`. Settings (`application.yml`, override with env vars):

| Setting | Env var | Default |
|---|---|---|
| `mdp.input-dir` | `MDP_INPUT_DIR` | `sample-data/input` |
| `mdp.output-dir` | `MDP_OUTPUT_DIR` | `output` |
| `mdp.chunk-size` | | `1000` |
| `mdp.skip-limit` | | `100` (more skipped records than this fails the run) |

## Try it

```bash
# Start a run (returns 202 immediately with the execution id)
curl -i -X POST localhost:8080/api/runs -H 'Content-Type: application/json' \
  -d '{"jobName":"fxFuturesEodJob","businessDate":"2026-11-11","triggeredBy":"alice"}'

# Check status, including read / write / skip counts per step
curl localhost:8080/api/runs/1

# Output
cat output/FX_FUTURE_EOD_2026-11-11.csv

# Health
curl localhost:8080/actuator/health
```

Behaviour of `POST /api/runs`:

* `202` with the run, `Location: /api/runs/{id}`; `runSequence` is computed (1 for the first run of a job + date, then 2, 3 ...).
* `404` unknown job, `400` invalid body (`businessDate` must be `yyyy-MM-dd`; any past date is allowed),
  `409` if a run of the same job + date is already running.
* `GET /api/runs/{id}`: `404` if unknown.

Rerunning a date overwrites its output file (plain overwrite semantics).

## Input and output files

* Input: `<input-dir>/<INSTRUMENT_TYPE>_<yyyy-MM-dd>.csv`, e.g. `sample-data/input/FX_FUTURE_2026-11-11.csv`
  with header `instrumentId,instrumentType,symbol,currency,maturityDate,closePrice`.
  The sample file contains three bad rows on purpose (missing price, negative price, non-numeric price).
* Output: `<output-dir>/<INSTRUMENT_TYPE>_EOD_<yyyy-MM-dd>.csv`, written as `.tmp` and atomically renamed
  only when the step succeeds. A failed run leaves no output.
* A missing input file fails the run. Invalid or unreadable rows are skipped, logged as WARN, and counted in
  the step (`readSkipCount`, `processSkipCount`).

## Project structure

```
pom.xml
sample-data/input/                      sample CSV
src/main/java/com/praxis/mdp/
  MarketDataPublisherApplication.java
  config/   MdpProperties (typed settings), JobsConfig (all jobs, in Java)
  model/    Instrument, EodPrice, MarketDataRecord, InstrumentType
  source/   MarketDataSource (interface), FileMarketDataSource (CSV)
  sink/     MarketDataSink + SinkWriter (interfaces), FileMarketDataSink (tmp file + atomic rename)
  batch/    EodJobFactory (builds a job per instrument type), MarketDataValidator (processor),
            skip + run logging listeners, RunParameters, lazy reader/writer wrappers
  api/      RunController, RunService (launch / sequence / duplicate guard), DTOs, error mapping
src/main/resources/application.yml, application-oracle.yml
src/test/java/...                       job integration test, API test, service unit test
```

Run identity: identifying job parameters `businessDate` + `runSequence`; non-identifying `triggeredBy`
and `triggerSource` (always `API` for now). Logs carry `run`, `job` and `date` from the MDC.

### Adding a job for another instrument type

In `JobsConfig` add one bean (the bean name is the job name used by the API):

```java
@Bean
Job fxForwardsEodJob(EodJobFactory f) {
    return f.create("fxForwardsEodJob", InstrumentType.FX_FORWARD);
}
```

and drop `FX_FORWARD_<date>.csv` in the input folder.

## Running against Oracle

```bash
export MDP_DB_URL=jdbc:oracle:thin:@//dbhost:1521/MDPSVC MDP_DB_USER=... MDP_DB_PASSWORD=...
java -jar target/market-data-publisher-0.1.0-SNAPSHOT.jar --spring.profiles.active=oracle
```

Create the Spring Batch tables once using `org/springframework/batch/core/schema-oracle.sql` from the
`spring-batch-core` jar (the profile sets `initialize-schema: never`). The Oracle profile is configured but
has **not** been tested against a real Oracle instance yet. Default (no profile) uses in-memory H2, so run
history resets on restart.

## What is stubbed / known limits

* Asset Control is replaced by `FileMarketDataSource`.
* One step writes the file directly from the source. There is no staging snapshot yet.
* No restart endpoint (a failed run is re-launched as a new run sequence).
* The "already running" guard is per process (a `synchronized` check against the job repository). Runs on two
  nodes at the same moment could both start; a failed JVM can leave an execution marked running, which blocks
  that date until it is cleaned up.
* No authentication/authorization; `triggeredBy` is taken from the request body.
* The run-sequence lookup scans the 1000 most recent instances of a job.
* Plain-text logs (JSON can come later).

## Next steps

1. Staging step: extract once to a local snapshot file, then one publish step per destination so a restart re-runs only the failed destination.
2. Kafka sink, SFTP sink (temp name + rename), REST sink - each implements `MarketDataSink` and is added as a step in `EodJobFactory`.
3. Real Asset Control `MarketDataSource` (page through instrument ids, resume position in the step context).
4. Restart endpoint and stale-execution cleanup on startup.
5. Security: OAuth2/OIDC resource server with roles (who may backdate or choose destinations), `triggeredBy` from the token; audit.
6. Autosys/Airflow integration docs, JSON logs, Micrometer/OpenTelemetry, Vault for secrets, Testcontainers Oracle tests, container image.
