# mysql datasource adapter

MySQL implementation of the relational datasource adapter contract.

## Usage Rules

- Construct `MySqlDatasourceConnector` in the application composition root and register it through `AdapterRegistry`.
- Provide a `JdbcConnectionFactory` that resolves `DatasourceConnection.bindingId()` through the host credential system.
- Keep passwords, tokens, connection pooling policy, and credential resolution outside the adapter configuration map.
- Use the discovery capability for databases, tables, views, and columns.
- Set discovery option `includeViews` to `true` or `false`; omitted values default to `false`.
- Treat returned external object IDs as opaque stable values and use qualified names only for display and search.
- Route relational queries through the integration runtime deadline and error-mapping contracts.
- Do not expose SQL text, credential values, or vendor error messages to callers.
- Run the datasource adapter contract tests for every supported connector configuration.
