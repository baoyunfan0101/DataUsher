# ai-runtime

`ai-runtime-api` defines conversations, provider calls, auditable tool
invocations, and runtime events. `ai-runtime-core` is selected by the
application composition root.

## Public Contracts

- Use `AiConversationService` to create conversations, append messages, and call providers.
- Use `AiToolInvocationService` to start and complete auditable tool invocations.
- Use `AiRuntimeEvents` for audit and operational projections.

## Usage Rules

- Route provider calls through LLM provider adapter interfaces only.
- Keep raw provider credentials, vendor SDK objects, and transport details outside runtime APIs.
- Use idempotency keys for tool invocations.
- Store typed tool arguments and results with `ExecutionValue` values.
- Subscribe to runtime events instead of reading internal runtime storage.
