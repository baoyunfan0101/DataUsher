# integration-kernel

## Usage Rules

- Business modules depend only on the required `*-adapters-api` project.
- The application composition root alone may depend on `integration-runtime-core`.
- Concrete adapters own vendor SDK calls and vendor-to-contract translation; vendor types must not cross an API boundary.
- Business modules own business state and workflows; adapters own no business lifecycle.
- Every business operation receives `AdapterRequestContext` and must honor its deadline.
- Declare supported operations with the module capability constants and register adapters through `AdapterRegistry`.
- Pass only `IntegrationValue` across dynamic data boundaries.
- Store only opaque secret-manager URIs in `CredentialBinding`; never put secret material in options or attributes.
- Map external failures with `IntegrationErrorMapper` before returning them to business modules.
