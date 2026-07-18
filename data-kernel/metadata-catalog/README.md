# metadata-catalog

## Owns

```text
Catalog, database, table, and column models
Stable metadata asset identifiers
Discovery synchronization
Metadata search
Current table schemas and immutable schema history
Metadata synchronization events
```

## Does not own

```text
External system discovery
JDBC or vendor SDK calls
Datasource credentials and connection testing
Query execution
Lineage, profiling, and data quality
```

The catalog consumes `DatasourceDiscoverySnapshot` values only. External object
identifiers remain adapter-defined, while internal IDs are stable SHA-256 keys
scoped by asset type and datasource. A full snapshot may use `REPLACE`; partial
snapshots must use `UPSERT` so unrelated metadata is never deleted.

Schema versions are appended only when the ordered column fingerprint changes.
