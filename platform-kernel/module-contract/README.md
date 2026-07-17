# module-contract

`module-contract-api` contains module identity, registry, and health contracts.
`module-contract-core` contains the default in-memory registry and health
aggregation service.

## Usage Rules

- Business modules depend on `module-contract-api` only.
- The application composition root selects and constructs `module-contract-core` implementations.
- Use stable canonical module, dependency, and capability names.
- Put static declarations in `ModuleDescriptor`; report runtime state through `ModuleHealthContributor`.
- Resolve dependency versions and health timeouts in the composition layer.
- Treat `UNKNOWN` as unavailable health evidence, not as `UP`.
