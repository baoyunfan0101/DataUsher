# execution-sql

Typed SQL workload and explain extensions for the language-neutral execution
lifecycle.

## Usage Rules

- Depend on `execution-sql-api` when a module needs to create SQL workloads or
  request SQL explain plans.
- Use `SqlWorkloads.statement` to create an `ExecutionWorkload`, then submit it
  through the standard `ExecutionCommandService` lifecycle.
- Construct `DefaultSqlExplainService` in the application composition root with
  an `ExecutionAccountQueryService`, adapter registry, invocation executor,
  clock, and adapter timeout.
- Select only active execution accounts that support `SqlWorkloads.TYPE`.
- The selected adapter must implement `SqlEngineAdapter` and declare the
  `compute.sql.explain` capability.
- Keep SQL-specific validation and adapter translation outside execution-core.
- Parameter naming conventions belong to the selected SQL adapter.
