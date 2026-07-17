# project-environment

`project-environment-api` contains project commands, project queries,
environment queries, and immutable project values. `project-environment-core`
contains the default services and local storage implementation.

## Usage Rules

- Business modules depend on `project-environment-api` only.
- The application composition root selects and constructs `project-environment-core` implementations.
- Supply at least one uniquely keyed environment when creating a project.
- Use project and environment IDs as scope identifiers; use display names only for presentation.
- Treat `ARCHIVED` as terminal and do not attempt to reactivate archived projects.
- Check project and environment status before creating scoped business state.
- Pass the caller's `RequestContext` to every project command.
