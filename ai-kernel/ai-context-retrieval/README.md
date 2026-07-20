# ai-context-retrieval

`ai-context-retrieval-api` defines permission-aware context queries and assembly
contracts for metadata, lineage, query, log, quality, and future sources.
`ai-context-retrieval-core` is selected by the application composition root.

## Public Contracts

- Use `AiContextQueryService` to retrieve governed context for a subject set.
- Use `AiContextAssembler` to build bounded prompt-ready context sections.
- Use `AiContextSourceRef` to keep source identity and resource references opaque.

## Usage Rules

- Provide subjects on every context query so resource checks can be enforced.
- Use source type filters to request categories without coupling to implementations.
- Bound retrieved items and assembled context before passing it to AI runtime.
- Preserve source references and attributes for traceability and audit projections.
