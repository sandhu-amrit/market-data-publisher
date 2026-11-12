# Spring Batch schema

Market Data Publisher stores run history only in Spring Batch's metadata tables. There is no application schema and no Flyway migration for business data.

## H2 (local profile)

`spring.batch.jdbc.initialize-schema` is `always` on the `local` profile and `embedded` by default, so the tables are created automatically.

## Oracle (dev / prod)

The `oracle` and `prod` profiles set `initialize-schema` to `never` unless `MDP_BATCH_SCHEMA_INIT` overrides it. A DBA applies the script shipped inside `spring-batch-core` once per schema:

```text
org/springframework/batch/core/schema-oracle.sql
```

After `./mvnw -q -DskipTests package` the jar is on the classpath. Extract the script with:

```bash
./mvnw -q dependency:copy \
  -Dartifact=org.springframework.batch:spring-batch-core:5.2.6 \
  -DoutputDirectory=target/batch-schema
unzip -p target/batch-schema/spring-batch-core-*.jar \
  org/springframework/batch/core/schema-oracle.sql > target/batch-schema/schema-oracle.sql
```

Run that file as the application user (`MDP_DB_USER`). Do not re-run it against a schema that already has the tables.

Set `MDP_BATCH_SCHEMA_INIT=always` only for a disposable schema (a first boot of Oracle Free in docker-compose). Leave it `never` in production so application startup cannot change the metadata DDL.

The Oracle profile also sets `spring.batch.jdbc.isolation-level-for-create=read_committed` to avoid `ORA-08177` when two runs are created at the same time.

`5.2.6` is the Spring Batch line managed by Spring Boot 3.5.16. If the copy fails, use the version printed by `./mvnw dependency:list -DincludeArtifactIds=spring-batch-core`.
