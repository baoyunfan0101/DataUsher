# execution-kernel

Stable execution lifecycle boundaries shared by interactive queries, scripts,
scheduled tasks, profiling, quality checks, and AI-triggered work.

| Project | Use for | Depends on |
| --- | --- | --- |
| `execution-core-api` | Execution requests, instances, queues, accounts, logs, and results | `shared-types-api` |
| `execution-core-core` | Default lifecycle orchestration and storage ports | `execution-core-api`, integration APIs, `shared-types-api` |
| `execution-sql-api` | SQL workload and explain contracts | `execution-core-api`, `shared-types-api` |
| `execution-sql-core` | SQL explain orchestration through typed compute adapters | execution and integration APIs |
| `workflow-core-api` | Versioned workflow graphs, schedules, publications, and runtime references | `execution-core-api`, governance and shared APIs |
| `workflow-core-core` | Default workflow lifecycle, scheduler publication, and execution-backed runtime | workflow, execution, and integration APIs |
| `development-lifecycle-api` | Versioned scripts, debug run references, and approval-backed publication | execution, workflow, governance, and shared APIs |
| `development-lifecycle-core` | Default development lifecycle services and replaceable stores | development, workflow, execution, governance, and shared APIs |

## Usage Rules

- Business and library modules depend on `execution-core-api` only.
- Only an application composition root may depend on `execution-core-core`.
- Submit an `ExecutionWorkload` for one run; keep reusable task definitions,
  versions, schedules, and workflow dependencies outside execution-core.
- Put reusable task graphs and schedules in workflow-core; let each task reference
  one `ExecutionSpecification` owned by execution-core.
- Put editable executable assets, debug runs, and approval-backed version promotion
  in development-lifecycle.
- Treat workload types and result modes as open values. Unknown values must be
  preserved by stores and relays.
- Store adapter IDs and credential binding IDs in execution accounts; never
  store secret material in execution requests, options, logs, or attributes.
- Keep platform request and instance IDs separate from external engine job IDs.
- Keep language-specific workload factories and operations in focused extension
  projects such as `execution-sql`; execution-core must remain language-neutral.
- Dispatch queued work outside the transaction that persists the request.
- Route all adapter calls through `AdapterInvocationExecutor`.
- Run `./gradlew verifyModuleBoundaries` after changing project dependencies.
