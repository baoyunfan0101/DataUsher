# execution-core

`execution-core-api` defines a language-neutral lifecycle for one executable
work item. `execution-core-core` provides the default orchestration and
replaceable storage ports.

## Lifecycle

An `ExecutionRequest` records execution intent and queue placement. An
`ExecutionInstance` records one actual attempt. A future task or workflow run
may create an execution request, but execution-core does not own reusable task
definitions, schedules, or DAGs.

```text
QUEUED -> DISPATCHING -> RUNNING -> SUCCEEDED
                                -> FAILED
                                -> CANCELLED
                                -> TIMED_OUT
```

Submission persists and queues work without waiting for the external engine.
An application worker calls `ExecutionWorker.dispatchNext`, then periodically
calls `ExecutionWorker.refresh` until the instance reaches a terminal state.

## Public Services

- `ExecutionCommandService` submits and cancels execution requests.
- `ExecutionQueryService` queries requests and instances.
- `ExecutionLogQueryService` reads logs with a resumable sequence cursor.
- `ExecutionResultQueryService` reads typed, paged results or result references.
- `ExecutionExplainService` requests an explain plan when the selected adapter
  declares the required capability.
- Queue and account command/query services manage concurrency boundaries and
  adapter bindings.

## Extension Rules

- Add a new workload with a new `ExecutionWorkloadType`; do not add a parallel
  execution lifecycle for each language or engine.
- An empty account workload type set means all current and future workload
  types. A non-empty set is an allowlist.
- Use `PAGED` for row results, `REFERENCE` for externally materialized output,
  and `DISCARD` when callers must not read a result. Custom result modes remain
  valid contract values.
- Use positional parameter names (`1`, `2`, and so on) only when required by a
  concrete adapter. Other adapters may define additional parameter conventions.
- Storage implementations must create and claim requests atomically, enforce
  queue concurrency, apply revision checks, and honor query paging.
- Do not expose vendor status objects, SDK handles, SQL exceptions, or raw
  credentials through execution-core APIs.
