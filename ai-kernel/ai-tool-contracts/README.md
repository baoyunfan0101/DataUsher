# ai-tool-contracts

`ai-tool-contracts-api` defines stable tool schemas, permission requirements,
catalog queries, and typed tool results. `ai-tool-contracts-core` is selected by
the application composition root.

## Public Contracts

- Use `AiToolRegistry` to register tool versions and change their status.
- Use `AiToolCatalogService` to discover active or historical tool definitions.
- Use `AiToolPermissionPolicy` to describe resource/action checks required before use.
- Use `AiToolResult` to return typed values without exposing implementation payloads.

## Usage Rules

- Treat tool IDs, parameter types, and attributes as open contract values.
- Pin invocations to immutable `AiToolRef` versions.
- Keep adapter, transport, prompt, and execution details outside tool schemas.
- Preserve unknown attributes so runtime and audit consumers can evolve independently.
