# shared-types

`shared-types-api` contains IDs, time, paging, request context, and domain event
contracts. `shared-types-core` contains JDK-backed default implementations.

## Usage Rules

- Business modules depend on `shared-types-api` only.
- The application composition root selects and constructs `shared-types-core` implementations.
- Inject `Clock` and `IdGenerator`; do not read time or generate IDs inside domain logic.
- Enforce query-specific page-size limits at the consuming boundary.
- Supply actor, request, and event time explicitly.
- Define event data as immutable types in the owning module's API project.
- Treat context attributes and paging collections as immutable snapshots.
- In-process event delivery is synchronous and attempts every subscriber before reporting failure.
