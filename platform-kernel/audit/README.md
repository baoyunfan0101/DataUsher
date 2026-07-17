# audit

`audit-api` contains resource-neutral audit commands, queries, and immutable
values. `audit-core` contains the default journal and storage port.

## Usage Rules

- Business modules depend on `audit-api` only.
- The application composition root selects and constructs `audit-core` implementations.
- Record immutable security and operational facts after state-changing decisions.
- Use `AuditTarget` instead of governance types so platform-kernel never depends upward.
- Keep database clients and storage technology outside the API project.
