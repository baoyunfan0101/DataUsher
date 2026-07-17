# configuration

`configuration-api` contains read-only configuration contracts.
`configuration-core` contains map-backed lookup and the default resolution
strategy.

## Usage Rules

- Business modules depend on `configuration-api` only.
- The application composition root selects and constructs `configuration-core` implementations.
- Use lowercase dot-separated keys and single-segment namespace and profile names.
- Defaults apply to missing values, not malformed typed values.
- Keep mutation, refresh, secrets, and external configuration clients outside the API project.
