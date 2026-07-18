# data-kernel

Stable datasource and metadata boundaries.

| Project | Use for | Depends on |
| --- | --- | --- |
| `datasource-connectivity-api` | Datasource lifecycle, queries, connection tests, and discovery snapshots | `shared-types-api` |
| `datasource-connectivity-core` | Default datasource orchestration and storage ports | `datasource-connectivity-api`, integration APIs, `shared-types-api` |
| `metadata-catalog-api` | Metadata synchronization, queries, search, and schema contracts | `datasource-connectivity-api`, `shared-types-api` |
| `metadata-catalog-core` | Default catalog services and storage ports | data-kernel APIs, `shared-types-api` |

## Usage Rules

- Business and library modules depend on `*-api` projects only.
- Only an application composition root may depend on `*-core` projects.
- A core project may depend on another module's API, never on another core.
- Keep credential values, vendor types, query execution, lineage, profiling, and quality execution outside data-kernel APIs.
- Treat identifiers, classification values, attributes, and fingerprints as opaque contract values.
- Preserve unknown classification values and attributes when relaying data between modules.
- Add new capabilities through new focused service interfaces instead of widening existing interfaces.
- Run `./gradlew verifyModuleBoundaries` after changing project dependencies.
