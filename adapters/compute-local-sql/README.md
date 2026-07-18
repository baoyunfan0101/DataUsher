# local SQL compute adapter

Managed local JDBC implementation of the SQL compute adapter contract.

## Usage Rules

- Construct `LocalSqlEngineAdapter` in the application composition root and
  register it through `AdapterRegistry`.
- Provide a `LocalSqlConnectionFactory` that resolves the supplied binding ID.
  Keep passwords, tokens, and connection-pool policy outside adapter options.
- Close the adapter during application shutdown so running statements and owned
  worker threads are cancelled and released.
- Submit SQL through the generic compute job lifecycle. The adapter supports
  status, cancellation, incremental logs, paged results, and text explain plans.
- Use positive JDBC parameter indexes (`1`, `2`, and so on) as parameter names.
- Set option `maxRows` to a positive integer no greater than `100000`; the
  default is `1000`.
- Treat result references and external job IDs as opaque values.
- Do not place SQL text, credentials, or sensitive parameter values in logs or
  error details.
- Run the managed compute and SQL adapter contract tests for each connection
  factory configuration.
