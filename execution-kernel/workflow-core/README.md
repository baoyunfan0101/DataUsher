# workflow-core

`workflow-core-api` defines reusable workflow definitions, immutable versions,
task graphs, schedules, policies, and runtime references. `workflow-core-core`
provides default lifecycle services and replaceable stores.

## Usage Rules

- Depend on `workflow-core-api` from business and library modules.
- Construct `workflow-core-core` only in an application composition root.
- Register the workflow `ResourceRef` before creating the workflow definition.
- Model task behavior with `WorkflowTaskAction`; use `ExecutionWorkflowTaskAction` for execution-core work.
- Use `AdapterWorkflowTaskAction` for adapter operations that must participate in workflow scheduling.
- Create a new immutable workflow version for every task, dependency, schedule, or policy change.
- Give each schedule a stable ID and status; a workflow version may contain multiple schedules.
- Select platform-managed or scheduler-managed runtime ownership explicitly on each version.
- Use open task, schedule, retry strategy, and dependency condition values for extensions.
- Keep task graphs acyclic and use workflow revisions for concurrent changes.
- Register matching task executors and scheduler task mappers for every custom task action.
- Publish scheduler-managed versions before triggering them and keep adapter bindings immutable per version.
- Trigger workflows idempotently, then let workers claim runs with renewable leases before dispatch.
- Report task progress through `ReportWorkflowTaskRunRequest`; execution events are one supported source.
- Refresh scheduler-managed runs to reconcile external workflow and task state.
- Resolve task logs and results through the runtime identified by the stored task run reference.
- Subscribe to typed workflow events for projections and integrations; use query services for current state.
