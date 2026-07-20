# ai-kernel

Stable AI tool, context, guardrail, and runtime contracts.

| Project | Use for | Depends on |
| --- | --- | --- |
| `ai-tool-contracts-api` | Tool schemas, permission requirements, catalog queries, and typed tool results | governance resource API, execution API, shared types |
| `ai-tool-contracts-core` | Default tool registry orchestration and replaceable storage ports | `ai-tool-contracts-api`, shared types |
| `ai-context-retrieval-api` | Permission-aware context queries and context assembly contracts | access/resource APIs, shared types |
| `ai-context-retrieval-core` | Context provider orchestration and permission filtering | `ai-context-retrieval-api`, access/resource APIs, shared types |
| `ai-guardrails-api` | Permission reviews, SQL safety reviews, and sensitive data filtering contracts | execution API, access/resource APIs, shared types |
| `ai-guardrails-core` | Default guardrail orchestration and replaceable review ports | `ai-guardrails-api`, access API, shared types |
| `ai-runtime-api` | Conversations, provider calls, auditable tool invocations, and lifecycle events | AI tool API, execution API, access API, LLM provider API, shared types |
| `ai-runtime-core` | Default runtime orchestration and replaceable storage ports | `ai-runtime-api`, LLM provider/runtime APIs, shared types |

## Usage Rules

- Business and library modules depend on `*-api` projects only.
- Only an application composition root may depend on `*-core` projects.
- AI modules call other modules through public service interfaces, never core implementations.
- Treat tool IDs, source types, finding types, and sensitive data types as open values.
- Require permission-aware context retrieval before exposing governed resources to AI workflows.
- Review SQL through `SqlSafetyReviewService` before AI-generated SQL reaches execution services.
- Filter sensitive content before persisting prompts, context, tool output, or provider responses.
- Route provider calls through `integration-kernel:llm-provider-adapters-api` only.
- Use runtime events for audit projections instead of reading internal runtime storage.
- Add new capabilities through focused service interfaces instead of widening existing ones.
