# development-lifecycle

`development-lifecycle-api` defines versioned development assets and persistent debug
run references. `development-lifecycle-core` provides default lifecycle services and
replaceable stores.

## Usage Rules

- Depend on `development-lifecycle-api` from business and library modules.
- Construct `development-lifecycle-core` only in an application composition root.
- Register the script `ResourceRef` before creating the script definition.
- Treat `ScriptLanguage` as an open classification value, not an execution capability list.
- Put executable content and engine requirements in `ExecutionSpecification`.
- Create immutable script versions and use script revisions for concurrent changes.
- Start debug runs idempotently, then let the development worker submit them to execution-core.
- Query debug logs, state, and results through execution-core using the stored request reference.
