# platform-kernel

Stable technical boundaries shared by DataUsher modules.

| Project | Use for | Depends on |
| --- | --- | --- |
| `shared-types-api` | IDs, time, paging, request context, and event contracts | None |
| `shared-types-core` | UUIDs, system time, and local event delivery | `shared-types-api` |
| `module-contract-api` | Module identity, registry, and health contracts | `shared-types-api` |
| `module-contract-core` | Default registry and health implementations | `module-contract-api`, `shared-types-api` |
| `configuration-api` | Read-only configuration contracts | None |
| `configuration-core` | Default configuration resolution | `configuration-api` |
| `audit-api` | Resource-neutral audit command and query contracts | `shared-types-api` |
| `audit-core` | Default audit journal and local storage | `audit-api`, `shared-types-api` |
| `observability-api` | Metrics and trace context contracts | None |
| `observability-core` | No-op metrics and thread-local trace context | `observability-api` |

## Usage Rules

- Business and library modules may depend on `*-api` projects only.
- Only an application composition root may depend on `*-core` projects.
- A core project may depend on another module's API, never on another core.
- Keep business concepts, persistence technology, and vendor types outside API projects.
- Treat returned records and collections as immutable values.
- Run `./gradlew verifyModuleBoundaries` after changing project dependencies.
