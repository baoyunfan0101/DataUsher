# data-kernel

Stable datasource, metadata, lineage, profiling, and quality boundaries.

| Project | Use for | Depends on |
| --- | --- | --- |
| `datasource-connectivity-api` | Datasource lifecycle, queries, connection tests, and discovery snapshots | `shared-types-api` |
| `datasource-connectivity-core` | Default datasource orchestration and storage ports | `datasource-connectivity-api`, integration APIs, `shared-types-api` |
| `metadata-catalog-api` | Metadata synchronization, queries, search, and schema contracts | `datasource-connectivity-api`, `shared-types-api` |
| `metadata-catalog-core` | Default catalog services and storage ports | data-kernel APIs, `shared-types-api` |
| `lineage-api` | Source-aware lineage ingestion, graph queries, traversal, and impact analysis | `shared-types-api` |
| `lineage-core` | Lineage lifecycle orchestration and replaceable storage ports | `lineage-api`, `shared-types-api` |
| `quality-profiler-api` | Profiling jobs, metrics, versioned quality rules, checks, and results | `execution-core-api`, `shared-types-api` |
| `quality-profiler-core` | Assessment orchestration and replaceable planner, decoder, and storage ports | `quality-profiler-api`, execution APIs, `shared-types-api` |

## Usage Rules

- Business and library modules depend on `*-api` projects only.
- Only an application composition root may depend on `*-core` projects.
- A core project may depend on another module's API, never on another core.
- Keep credential values, vendor SDK types, engine payloads, and workflow scheduling outside data-kernel APIs.
- Treat identifiers, classification values, attributes, and fingerprints as opaque contract values.
- Preserve unknown classification values and attributes when relaying data between modules.
- Add new capabilities through new focused service interfaces instead of widening existing interfaces.
- Run `./gradlew verifyModuleBoundaries` after changing project dependencies.
