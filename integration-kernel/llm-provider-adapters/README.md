# llm-provider-adapters

`llm-provider-adapters-api` defines provider-neutral chat and embedding adapter
contracts. `llm-provider-adapters-contract-tests` provides reusable contract
fixtures for adapter implementations.

## Public Contracts

- Implement `LlmProviderAdapter` for chat-capable providers.
- Implement `EmbeddingProviderAdapter` for embedding-capable providers.
- Use contract fixtures to verify provider identity, capabilities, response shape, and sensitive value safety.

## Usage Rules

- Keep provider credentials and SDK-native objects outside public adapter contracts.
- Preserve requested model identity in responses.
- Map provider failures to integration runtime failures without leaking sensitive values.
- Declare every supported capability in the adapter descriptor.
