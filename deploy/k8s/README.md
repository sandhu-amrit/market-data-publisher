# Kubernetes notes

The process is a single VM under systemd today. The same image (`deploy/Dockerfile`) runs unmodified as one replica. Do not scale past one replica until `ClusterLock` is backed by ShedLock or a lease: two schedulers can both pass the duplicate-run check.

Suggested pod settings:

- `terminationGracePeriodSeconds: 40` so `server.shutdown=graceful` and the job executor can finish the in-flight chunk (`spring.lifecycle.timeout-per-shutdown-phase=30s`).
- `runAsNonRoot: true`, `runAsUser: 10001`, read-only root filesystem with writable `emptyDir` mounts on `/app/work` and `/app/output`.
- Probes: `GET /actuator/health/liveness` and `GET /actuator/health/readiness` (unauthenticated). Prometheus scrapes `/actuator/prometheus`; restrict it with a network policy.
- Env from a Secret or a Vault agent injector, never from the image: `MDP_DB_URL`, `MDP_DB_USER`, `MDP_DB_PASSWORD`, `MDP_JWT_ISSUER`, `MDP_SFTP_PASSWORD`, `MDP_REST_TOKEN`, `MDP_KAFKA_BOOTSTRAP`.
- Profile: `SPRING_PROFILES_ACTIVE=prod`.
- One container, no sidecar required. An OpenTelemetry collector is optional: set `OTEL_EXPORT_ENABLED=true` and `OTEL_EXPORTER_OTLP_ENDPOINT`.

Autosys or Airflow stays outside the cluster and calls `POST /api/v1/runs` with an `MDP_OPERATOR` token. The in-process cron is optional and should be disabled (`mdp.jobs.<name>.enabled=false` or `schedule: none`) when an external scheduler owns the calendar, so the job is not triggered twice.
