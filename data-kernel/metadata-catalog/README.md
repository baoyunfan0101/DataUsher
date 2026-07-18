# metadata-catalog

`metadata-catalog-api` contains synchronization, metadata query, search, schema,
and immutable value contracts. `metadata-catalog-core` contains the default
catalog services and storage ports.

## Usage Rules

- Business modules depend on `metadata-catalog-api` only.
- The application composition root selects `metadata-catalog-core` implementations.
- Synchronize only normalized `DatasourceDiscoverySnapshot` values; never call vendor APIs from the catalog.
- Use `REPLACE` only with a full datasource snapshot and `UPSERT` with a partial snapshot.
- Treat `MetadataId` and schema fingerprints as opaque stable values.
- An empty search type set means all current and future asset types.
- Preserve custom `MetadataAssetType`, `TableKind`, and unknown attributes for forward compatibility.
- Use the ordered column list as the schema contract; schema history is append-only.
- Storage implementations must apply each synchronization atomically and preserve prior state on failure.
- Storage implementations own filtering, relevance ordering, and paging while honoring the public search query.
- Keep query execution, lineage, profiling, quality results, and vendor types outside this module.
