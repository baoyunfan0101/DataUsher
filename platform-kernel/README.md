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
| `notification-api` | Channel-neutral templates, recipients, dispatches, and provider contracts | `shared-types-api` |
| `notification-core` | Default template rendering and reliable dispatch lifecycle | `notification-api`, `audit-api`, `shared-types-api` |

## Usage Rules

- Business and library modules may depend on `*-api` projects only.
- Only an application composition root may depend on `*-core` projects.
- A core project may depend on another module's API, never on another core.
- Keep business concepts, persistence technology, and vendor types outside API projects.
- Keep delivery channels in notification templates and providers, not in business send requests.
- Reuse a notification idempotency key only for the same logical request.
- Treat returned records and collections as immutable values.
- Run `./gradlew verifyModuleBoundaries` after changing project dependencies.
