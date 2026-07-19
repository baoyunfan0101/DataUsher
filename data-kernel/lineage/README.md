# lineage

`lineage-api` defines source-aware graph ingestion, node and edge queries,
bounded traversal, impact analysis, and lineage events. `lineage-core` is
selected by the application composition root.

## Usage Rules

- Business and library modules depend on `lineage-api` only.
- Model direction from the upstream producer or input to the downstream consumer or output.
- Use open node, edge, and source types; preserve unknown values and attributes.
- Identify a producer with `LineageSourceRef` and increase its source revision monotonically.
- Submit every edge with both endpoint declarations in the same atomic snapshot.
- Use `UPSERT` for partial evidence and `REPLACE` for a complete source snapshot.
- Reusing the same revision is idempotent only when the normalized snapshot is unchanged.
- Treat node and edge IDs as opaque values; resolve external assets through `LineageNodeRef`.
- Bound every traversal by depth and node count before exposing it to interactive consumers.
- Use downstream impact analysis for operational and change-management decisions.
- Subscribe to `LineageUpdatedEvent` for projections instead of reading internal storage.
