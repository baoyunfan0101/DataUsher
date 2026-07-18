# notification

`notification-api` provides channel-neutral template, recipient, dispatch,
delivery, receipt, query, retry, and provider contracts. `notification-core` provides the
default dispatch lifecycle.

## Usage Rules

- Depend on `notification-api` from business and library modules.
- Construct `notification-core` only in an application composition root.
- Put channels and channel-specific content in versioned templates.
- Send only a template key, recipients, parameters, attributes, and an idempotency key.
- Register one `NotificationChannelProvider` for each channel used by active templates.
- Treat the provider envelope idempotency key as the stable key for one recipient and channel delivery.
- Reuse a send idempotency key only when template, recipients, parameters, and attributes are unchanged.
- Treat a provider response as acceptance, not proof of recipient delivery.
- Confirm delivery from a provider receipt using the dispatch, channel, and provider reference.
- Retry a dispatch to redeliver only pending or failed deliveries; accepted and delivered records are preserved.
- Subscribe to typed delivery events instead of inferring lifecycle changes from storage records.
- Configure a durable `DomainEventPublisher` when notification events must survive process failure.
- Pass the caller's `RequestContext` to template, send, retry, and receipt commands.
