# hive datasource adapter

Hive implementation of the relational datasource adapter contract.

## Usage Rules

- Construct `HiveDatasourceConnector` in the application composition root and register it through `AdapterRegistry`.
- Provide a `HiveJdbcConnectionFactory` that resolves `DatasourceConnection.bindingId()` through the host credential system.
- Keep Hive JDBC URLs, passwords, tokens, and connection-pool policy outside adapter request payloads.
- Use the adapter to discover Hive databases, tables, views, and columns and to validate serving queries when needed.
- Treat returned external object IDs as opaque stable values and use qualified names only for display and search.
- Route batch transformations through a compute adapter such as Spark SQL; Hive is the offline storage boundary in the pipeline.
- Do not expose SQL text, credential values, or vendor error messages to callers.
- Run the datasource adapter contract tests for every supported connector configuration.
