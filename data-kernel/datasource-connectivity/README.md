# datasource-connectivity

`datasource-connectivity-api` contains datasource commands, queries, connection
tests, discovery snapshots, and immutable values. `datasource-connectivity-core`
contains the default orchestration and storage ports.

## Usage Rules

- Business modules depend on `datasource-connectivity-api` only.
- The application composition root selects `datasource-connectivity-core` implementations and registers datasource adapters.
- Store only non-secret connection properties and an opaque `credentialBindingId` in a datasource definition.
- Resolve credential values in the integration layer; never place them in discovery options or object attributes.
- Use `expectedRevision` for status changes and treat revision conflicts as failed commands.
- Test connections and discover metadata only for active datasources.
- Treat discovery object IDs as stable adapter-owned identifiers, not display names.
- Preserve custom `DiscoveredObjectKind` values and unknown attributes for forward compatibility.
- An empty discovery namespace represents a full datasource scope; a non-empty namespace represents a partial scope.
- Storage implementations must provide atomic create and compare-and-set update behavior and must honor query paging.
