# Seckill Perf Validation

This folder contains two separate load-test plans:

- `seckill-core-load-test.jmx`
  Measures the Redis/Lua/RabbitMQ seckill path through the perf-only endpoint enabled by the `perf` Spring profile.
  This isolates the seckill hot path from JWT authentication overhead.

- `seckill-grab-load-test.jmx`
  Measures the authenticated production-style API path.

Recommended verification flow:

1. Start the backend with `dev,perf` profiles.
2. Run `campus-life-backend/scripts/run-seckill-perf.ps1`.
3. Review the generated summary JSON, JTL, and HTML report.

The generated summary reports both:

- HTTP-layer throughput from JMeter
- service-level submit and accepted QPS from `/api/admin/seckill/health`

Use the service-level counters when you want to prove the Redis/Lua/MQ chain itself, and the authenticated API plan when you want a production-like end-to-end figure.
