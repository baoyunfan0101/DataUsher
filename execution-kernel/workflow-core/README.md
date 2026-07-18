# workflow-core

`workflow-core-api` defines reusable workflow definitions, immutable versions,
task graphs, schedules, policies, and runtime references. `workflow-core-core`
provides default lifecycle services and replaceable stores.

## Usage Rules

- Depend on `workflow-core-api` from business and library modules.
- Construct `workflow-core-core` only in an application composition root.
- Register the workflow `ResourceRef` before creating the workflow definition.
- Put executable task content in `ExecutionSpecification`; do not define another execution lifecycle.
- Create a new immutable workflow version for every task, dependency, schedule, or policy change.
- Use open schedule and dependency condition values for adapter-specific extensions.
- Keep task graphs acyclic and use workflow revisions for concurrent changes.
- Trigger workflows idempotently, then let `WorkflowWorker` dispatch ready tasks.
- Use `ExecutionStateChangedEvent` to advance task dependencies and retries.
- Query task logs and results through execution-core using the stored execution request reference.
- Subscribe to typed workflow events for projections and integrations; use query services for current state.
