# spark SQL compute adapter

Spark SQL implementation of the compute engine adapter contract.

## Usage Rules

- Construct `SparkSqlEngineAdapter` in the application composition root and register it through `AdapterRegistry`.
- Provide a `SparkSqlConnectionFactory` that resolves the supplied binding ID through the host credential system.
- Keep Spark Thrift Server URLs, passwords, tokens, and connection pooling outside adapter request payloads.
- Submit SQL through the generic compute job lifecycle. The adapter supports status, logs, paged results, and text explain plans.
- Use positive JDBC parameter indexes (`1`, `2`, and so on) as parameter names.
- Set option `maxRows` to a positive integer no greater than `100000`; the default is `1000`.
- Treat result references and external job IDs as opaque values.
- Do not place SQL text, credentials, or sensitive parameter values in logs or error details.
- Run the managed compute and SQL adapter contract tests for each Spark connection factory configuration.
