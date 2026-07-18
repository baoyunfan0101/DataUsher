# approval

`approval-api` provides reusable approval template, request, decision, query,
approver selector, and terminal callback contracts. `approval-core` provides the
default lifecycle services.

## Usage Rules

- Depend on `approval-api` from business and library modules.
- Construct `approval-core` only in an application composition root.
- Publish a new template version for every template change.
- Submit by template key; each request records the selected version and resolved step snapshot.
- Reuse a submission idempotency key only for the same logical approval request.
- Register an `ApproverSelectorResolver` for every selector type used by a template.
- Use resource-owner selectors when approval responsibility follows resource ownership.
- Address every decision to an explicit template step key.
- Register callback handlers before requests of that callback type reach terminal callback delivery.
- Make callback handlers idempotent by `ApprovalRequestId` and use the correlation key for business lookup.
- Query callback delivery state and retry failed deliveries through `ApprovalCallbackDeliveryService`.
- Pass the caller's `RequestContext` to template, request, decision, cancel, and retry commands.
