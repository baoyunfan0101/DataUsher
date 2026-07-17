# identity-access

`identity-access-api` contains subject lookup and access-decision contracts.
`identity-access-core` contains the default identity lookup, policy evaluation,
and local storage implementations.

## Usage Rules

- Business modules depend on `identity-access-api` only.
- The application composition root selects and constructs `identity-access-core` implementations.
- Include the caller and all effective groups or service identities in `AccessRequest.subjects`.
- Use only actions declared by the target resource type.
- Treat every decision other than `ALLOWED` as denied.
- Use `AccessDecisionCode` for control flow and `reason` for diagnostics.
- Expect default denial for unknown, disabled, inactive, or unmatched inputs.
- Pass the caller's `RequestContext` to every access decision.
