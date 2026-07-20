# ai-guardrails

`ai-guardrails-api` defines permission reviews, SQL safety reviews, and sensitive
data filtering contracts. `ai-guardrails-core` is selected by the application
composition root.

## Public Contracts

- Use `AiGuardrailService` to review subject, action, and resource access.
- Use `SqlSafetyReviewService` before executing AI-generated SQL.
- Use `SensitiveDataFilter` before storing or sending sensitive text.

## Usage Rules

- Treat finding types and sensitive data types as open values.
- Return findings with stable severity and decision codes rather than provider text.
- Keep detector, parser, and policy implementation details outside public requests.
- Prefer blocking decisions when safety cannot be determined by available policies.
