# data-kernel

## Responsibility

`data-kernel` owns the platform representation of data sources and data assets.
It converts external discovery results into stable, reusable platform metadata.

## Module boundaries

```text
datasource-connectivity
    Owns datasource definitions and lifecycle.
    Orchestrates connector calls through integration-kernel ports.
    Publishes normalized discovery snapshots.
    Never stores credentials or implements vendor protocols.

metadata-catalog
    Owns catalogs, databases, tables, columns, search documents, and schemas.
    Consumes normalized discovery snapshots from datasource-connectivity.
    Never calls JDBC, vendor SDKs, or credential services.
```

The kernel does not own SQL execution, workflow scheduling, connector drivers,
credential values, access control, lineage, profiling, or quality execution.

No data-kernel API exposes JDBC or MySQL types.
