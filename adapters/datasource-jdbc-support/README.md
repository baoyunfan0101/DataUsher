# JDBC datasource support

Shared JDBC implementation support for concrete relational datasource adapters.

## Usage Rules

- Use this project only from concrete datasource adapter modules.
- Keep vendor-specific adapter IDs, capabilities, options, and README files in the concrete adapter module.
- Provide a `JdbcRelationalConnectionFactory` that resolves binding IDs through the host credential system.
- Do not expose this support project to business modules or application workflows.
