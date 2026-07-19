# quality-profiler

`quality-profiler-api` defines profiling jobs, typed metrics, immutable quality
rule versions, quality checks, results, and lifecycle events.
`quality-profiler-core` is selected by the application composition root.

## Usage Rules

- Business and library modules depend on `quality-profiler-api` only.
- Treat data target, metric, rule, severity, and assessment execution types as open values.
- Start profile jobs and quality checks idempotently, then let workers dispatch pending work.
- Register an execution planner for every supported assessment execution type.
- Register a result decoder for every workload type emitted by those planners.
- Keep engine language and payload details inside planners and decoders, not public requests.
- Pin every quality check to immutable `QualityRuleRef` versions.
- Disable or archive a rule without rewriting its prior versions or historical results.
- Interpret a successful check run as successful evaluation, not as every rule passing.
- Use `QualityResult.outcome` for rule decisions and `DataAnomalyDetectedEvent` for anomalies.
- Query execution logs through execution-core using the stored execution request ID.
- Preserve unknown result attributes so operation and AI consumers can evolve independently.
