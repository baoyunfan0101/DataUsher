# integration-kernel

## Usage Rules

- Business modules depend only on the required `*-adapters-api` project.
- The application composition root alone may depend on `integration-runtime-core`.
- Concrete adapters own vendor SDK calls and vendor-to-contract translation; vendor types must not cross an API boundary.
- Business modules own business state and workflows; adapters own no business lifecycle.
- Route adapter calls through `AdapterInvocationExecutor` to enforce request deadlines.
- Model compute work as managed jobs: submit once, retain the opaque handle, and
  use capability-gated status, cancellation, log, result, and explain operations.
- Publish scheduler workflows with portable task, dependency, and schedule contracts.
- Preserve trigger idempotency keys in scheduler run handles.
- Observe scheduler task runs through the paged task-run contract.
- Publish visualization dataset, chart, and dashboard bindings through `VisualizationAdapter`.
- Concrete adapters must propagate interruption to vendor calls when cancellation is supported.
- Declare supported operations with the module capability constants and register adapters through `AdapterRegistry`.
- Pass only `IntegrationValue` across dynamic data boundaries.
- Store only opaque secret-manager URIs in `CredentialBinding`; never put secret material in options or attributes.
- Map external failures with `IntegrationErrorMapper` before returning them to business modules.
- Run the applicable `adapter-contract-tests` checks for every concrete adapter.
