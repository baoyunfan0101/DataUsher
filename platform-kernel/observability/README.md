# observability

`observability-api` contains metrics and trace context contracts.
`observability-core` contains no-op metrics and scoped thread-local context.

## Usage Rules

- Business modules depend on `observability-api` only.
- The application composition root selects and constructs `observability-core` implementations.
- Use stable metric names and bounded-cardinality tags.
- Close trace scopes on the owning thread in reverse order.
- Propagate trace context explicitly across asynchronous boundaries.
- Keep telemetry vendor types and SDKs outside the API project.
