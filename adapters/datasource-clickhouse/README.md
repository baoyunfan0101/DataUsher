# clickhouse datasource adapter

ClickHouse implementation of the relational datasource adapter contract.

## Usage Rules

- Construct `ClickHouseDatasourceConnector` in the application composition root and register it through `AdapterRegistry`.
- Provide a `ClickHouseJdbcConnectionFactory` that resolves `DatasourceConnection.bindingId()` through the host credential system.
- Keep ClickHouse JDBC URLs, passwords, tokens, and connection-pool policy outside adapter request payloads.
- Use the adapter to discover serving databases, tables, views, and columns for dashboard binding.
- Route dashboard queries through ClickHouse datasource/query contracts, not through the offline Hive storage boundary.
- Treat returned external object IDs as opaque stable values and use qualified names only for display and search.
- Do not expose SQL text, credential values, or vendor error messages to callers.
- Run the datasource adapter contract tests for every supported connector configuration.
