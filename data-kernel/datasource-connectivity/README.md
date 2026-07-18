# datasource-connectivity

## Owns

```text
Datasource identity and display metadata
Connector and credential binding references
Non-secret connection properties
Datasource lifecycle and optimistic revision
Connection-test orchestration
Discovery orchestration and normalized discovery snapshots
Datasource lifecycle and discovery events
```

## Does not own

```text
Credential values or secret resolution
JDBC connections and vendor drivers
Catalog, table, column, and schema persistence
SQL result execution lifecycle
Connector registration implementation
```

The API module contains stable commands, queries, value objects, and service
ports. The core module contains orchestration and persistence ports. In-memory
stores are reference implementations for tests and local composition; durable
stores can implement the same ports without changing the public API.

Sensitive connection property names are rejected. Runtime credentials must be
resolved from `credentialBindingId` by the host-provided connector factory.
