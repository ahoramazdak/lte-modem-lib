Integration Stress Test (Reactive)

This README explains how to run the integration serial stress test locally and in CI.

Running locally

- Build and run the single integration test (default settings, replay sink):

```bash
mvn -DskipTests=false test -Dtest=IntegrationSerialStressTest#runStress
```

- Override defaults via system properties:

  - `-Dstress.events=10000` (number of events to send)
  - `-Dstress.rateMs=1` (delay between writes in ms)
  - `-Dstress.timeoutSec=120` (timeout in seconds)
  - `-Dreactor.sink.strategy=multicast` (switch sink strategy: `replay` or `multicast`)
  - `-Dci=true` (treat run as CI; defaults reduce load)

CI recommendations

- On CI, set `CI=true` in the environment (or `-Dci=true`) so the test uses a smaller default event count (500).
- Prefer the `replay` sink in CI for deterministic captures; use `multicast` locally when testing drop/backpressure behavior.

Interpreting results

- The test prints a short report with event count, total time, latency (min/avg/max in ms), and throughput (events/sec).
- Compare `replay` vs `multicast` by running with `-Dreactor.sink.strategy=multicast`.

Notes

- The reactive sink is configurable in `ReactiveEventDispatcher` using system property `reactor.sink.strategy`.
- The `LteModemManager.testEmitEvent(...)` helper is present to allow emitting synthetic events in tests. It is guarded to ignore null events.

Contact

If you want me to add automatic comparison scripts or CI job snippets, tell me which CI system you use (GitHub Actions, GitLab CI, Jenkins, etc.) and I'll add a ready-to-use job file.
