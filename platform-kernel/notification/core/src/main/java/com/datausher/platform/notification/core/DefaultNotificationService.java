package com.datausher.platform.notification.core;

import com.datausher.platform.audit.api.AuditCommandService;
import com.datausher.platform.audit.api.AuditOutcome;
import com.datausher.platform.audit.api.AuditRecordRequest;
import com.datausher.platform.audit.api.AuditTarget;
import com.datausher.platform.audit.api.AuditedCommand;
import com.datausher.platform.audit.api.AuditedCommandExecutor;
import com.datausher.platform.notification.api.NotificationChannel;
import com.datausher.platform.notification.api.NotificationChannelProvider;
import com.datausher.platform.notification.api.NotificationChannelResult;
import com.datausher.platform.notification.api.NotificationCommandService;
import com.datausher.platform.notification.api.NotificationContent;
import com.datausher.platform.notification.api.NotificationDelivery;
import com.datausher.platform.notification.api.NotificationDeliveryStatus;
import com.datausher.platform.notification.api.NotificationDispatch;
import com.datausher.platform.notification.api.NotificationDispatchId;
import com.datausher.platform.notification.api.NotificationDispatchStatus;
import com.datausher.platform.notification.api.NotificationEnvelope;
import com.datausher.platform.notification.api.NotificationQueryService;
import com.datausher.platform.notification.api.NotificationRecipient;
import com.datausher.platform.notification.api.NotificationRoute;
import com.datausher.platform.notification.api.NotificationTemplate;
import com.datausher.platform.notification.api.NotificationTemplateKey;
import com.datausher.platform.notification.api.PublishNotificationTemplateRequest;
import com.datausher.platform.notification.api.RetryNotificationRequest;
import com.datausher.platform.notification.api.SendNotificationRequest;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DefaultNotificationService implements NotificationCommandService, NotificationQueryService {
    private static final int MAX_ERROR_LENGTH = 500;

    private final NotificationStore store;
    private final NotificationTemplateRenderer renderer;
    private final Map<NotificationChannel, NotificationChannelProvider> providers;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final AuditedCommandExecutor commandExecutor;
    private final AuditCommandService audit;

    public DefaultNotificationService(
            NotificationStore store,
            NotificationTemplateRenderer renderer,
            Collection<? extends NotificationChannelProvider> providers,
            IdGenerator idGenerator,
            Clock clock,
            AuditedCommandExecutor commandExecutor,
            AuditCommandService audit
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer must not be null");
        this.providers = indexProviders(providers);
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
        this.audit = Objects.requireNonNull(audit, "audit must not be null");
    }

    @Override
    public NotificationTemplate publishTemplate(PublishNotificationTemplateRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        long version = store.findLatestTemplate(request.templateKey())
                .map(template -> template.version() + 1)
                .orElse(1L);
        NotificationTemplate template = new NotificationTemplate(
                request.templateKey(), version, request.displayName(), request.routes(),
                clock.now(), request.requestContext().actor().actorId(), request.attributes());
        return commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public NotificationTemplate execute() {
                store.createTemplate(template);
                return template;
            }

            @Override
            public AuditRecordRequest audit(NotificationTemplate result) {
                return new AuditRecordRequest(
                        request.requestContext(), "notification", "notification-template.publish",
                        AuditTarget.global("notification-template", result.templateKey().value()),
                        AuditOutcome.SUCCEEDED, Map.of("version", Long.toString(result.version())));
            }

            @Override
            public void rollback(NotificationTemplate result, RuntimeException cause) {
                store.deleteTemplate(result);
            }
        });
    }

    @Override
    public NotificationDispatch send(SendNotificationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String fingerprint = fingerprint(request);
        Optional<StoredNotificationDispatch> existing = store.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            if (!existing.orElseThrow().requestFingerprint().equals(fingerprint)) {
                throw new IllegalStateException("idempotency key was used for a different notification request");
            }
            return existing.orElseThrow().dispatch();
        }
        NotificationTemplate template = store.findLatestTemplate(request.templateKey())
                .orElseThrow(() -> new IllegalArgumentException(
                        "notification template does not exist: " + request.templateKey()));
        StoredNotificationDispatch initial = createDispatch(template, request, fingerprint);
        StoredNotificationDispatch stored = commandExecutor.execute(new AuditedCommand<>() {
            @Override
            public StoredNotificationDispatch execute() {
                store.createDispatch(initial);
                return initial;
            }

            @Override
            public AuditRecordRequest audit(StoredNotificationDispatch result) {
                return dispatchAudit(request.requestContext(), "notification.dispatch.create",
                        result.dispatch(), AuditOutcome.SUCCEEDED);
            }

            @Override
            public void rollback(StoredNotificationDispatch result, RuntimeException cause) {
                store.deleteDispatch(result);
            }
        });
        return deliverOutstanding(stored, request.requestContext()).dispatch();
    }

    @Override
    public NotificationDispatch retry(RetryNotificationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        StoredNotificationDispatch stored = store.findDispatch(request.dispatchId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "notification dispatch does not exist: " + request.dispatchId()));
        return deliverOutstanding(stored, request.requestContext()).dispatch();
    }

    @Override
    public Optional<NotificationTemplate> findTemplate(NotificationTemplateKey templateKey, long version) {
        return store.findTemplate(Objects.requireNonNull(templateKey, "templateKey must not be null"), version);
    }

    @Override
    public Optional<NotificationTemplate> findLatestTemplate(NotificationTemplateKey templateKey) {
        return store.findLatestTemplate(Objects.requireNonNull(templateKey, "templateKey must not be null"));
    }

    @Override
    public List<NotificationTemplate> listTemplateVersions(NotificationTemplateKey templateKey) {
        return store.listTemplateVersions(Objects.requireNonNull(templateKey, "templateKey must not be null"));
    }

    @Override
    public Optional<NotificationDispatch> findDispatch(NotificationDispatchId dispatchId) {
        return store.findDispatch(Objects.requireNonNull(dispatchId, "dispatchId must not be null"))
                .map(StoredNotificationDispatch::dispatch);
    }

    @Override
    public Optional<NotificationDispatch> findByIdempotencyKey(String idempotencyKey) {
        String normalized = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        return store.findByIdempotencyKey(normalized).map(StoredNotificationDispatch::dispatch);
    }

    private StoredNotificationDispatch createDispatch(
            NotificationTemplate template,
            SendNotificationRequest request,
            String fingerprint
    ) {
        NotificationDispatchId dispatchId = new NotificationDispatchId(idGenerator.nextIdValue(
                IdGenerationRequest.of("platform", "notification-dispatch")));
        List<NotificationDelivery> deliveries = new ArrayList<>();
        Map<String, NotificationContent> contents = new HashMap<>();
        for (NotificationRecipient recipient : request.recipients()) {
            for (NotificationRoute route : template.routes()) {
                NotificationDelivery delivery = new NotificationDelivery(
                        recipient, route.channel(), NotificationDeliveryStatus.PENDING,
                        0, null, "", "");
                deliveries.add(delivery);
                contents.put(deliveryKey(recipient, route.channel()),
                        renderer.render(route.content(), request.parameters()));
            }
        }
        NotificationDispatch dispatch = new NotificationDispatch(
                dispatchId, template.templateKey(), template.version(), request.idempotencyKey(),
                NotificationDispatchStatus.PENDING, deliveries, clock.now(), null, request.attributes());
        return new StoredNotificationDispatch(dispatch, fingerprint, contents);
    }

    private synchronized StoredNotificationDispatch deliverOutstanding(
            StoredNotificationDispatch initial,
            RequestContext context
    ) {
        StoredNotificationDispatch current = store.findDispatch(initial.dispatch().dispatchId()).orElseThrow();
        for (int index = 0; index < current.dispatch().deliveries().size(); index++) {
            NotificationDelivery delivery = current.dispatch().deliveries().get(index);
            if (delivery.status() == NotificationDeliveryStatus.SUCCEEDED) {
                continue;
            }
            NotificationDelivery attempted = deliver(current, delivery, context);
            List<NotificationDelivery> nextDeliveries = new ArrayList<>(current.dispatch().deliveries());
            nextDeliveries.set(index, attempted);
            NotificationDispatch nextDispatch = withDeliveries(current.dispatch(), nextDeliveries);
            StoredNotificationDispatch next = new StoredNotificationDispatch(
                    nextDispatch, current.requestFingerprint(), current.renderedContents());
            store.updateDispatch(current, next);
            current = next;
        }
        AuditOutcome outcome = current.dispatch().status() == NotificationDispatchStatus.SUCCEEDED
                ? AuditOutcome.SUCCEEDED : AuditOutcome.FAILED;
        audit.record(dispatchAudit(context, "notification.dispatch.complete", current.dispatch(), outcome));
        return current;
    }

    private NotificationDelivery deliver(
            StoredNotificationDispatch stored,
            NotificationDelivery delivery,
            RequestContext context
    ) {
        Instant attemptedAt = clock.now();
        int attempts = delivery.attempts() + 1;
        try {
            NotificationChannelProvider provider = providers.get(delivery.channel());
            if (provider == null) {
                throw new IllegalStateException("no notification provider is registered for: " + delivery.channel());
            }
            NotificationContent content = stored.renderedContents().get(
                    deliveryKey(delivery.recipient(), delivery.channel()));
            NotificationChannelResult result = Objects.requireNonNull(provider.deliver(new NotificationEnvelope(
                    stored.dispatch().dispatchId(), deliveryIdempotencyKey(stored.dispatch(), delivery),
                    delivery.recipient(), delivery.channel(), content, context)),
                    "notification provider result must not be null");
            return new NotificationDelivery(
                    delivery.recipient(), delivery.channel(), NotificationDeliveryStatus.SUCCEEDED,
                    attempts, attemptedAt, result.providerReference(), "");
        } catch (RuntimeException exception) {
            return new NotificationDelivery(
                    delivery.recipient(), delivery.channel(), NotificationDeliveryStatus.FAILED,
                    attempts, attemptedAt, "", safeError(exception));
        }
    }

    private NotificationDispatch withDeliveries(
            NotificationDispatch dispatch,
            List<NotificationDelivery> deliveries
    ) {
        long succeeded = deliveries.stream()
                .filter(delivery -> delivery.status() == NotificationDeliveryStatus.SUCCEEDED).count();
        long pending = deliveries.stream()
                .filter(delivery -> delivery.status() == NotificationDeliveryStatus.PENDING).count();
        NotificationDispatchStatus status;
        if (pending > 0) {
            status = NotificationDispatchStatus.PENDING;
        } else if (succeeded == deliveries.size()) {
            status = NotificationDispatchStatus.SUCCEEDED;
        } else if (succeeded > 0) {
            status = NotificationDispatchStatus.PARTIALLY_SUCCEEDED;
        } else {
            status = NotificationDispatchStatus.FAILED;
        }
        return new NotificationDispatch(
                dispatch.dispatchId(), dispatch.templateKey(), dispatch.templateVersion(), dispatch.idempotencyKey(),
                status, deliveries, dispatch.createdAt(),
                status == NotificationDispatchStatus.PENDING ? null : clock.now(), dispatch.attributes());
    }

    private static AuditRecordRequest dispatchAudit(
            RequestContext context,
            String action,
            NotificationDispatch dispatch,
            AuditOutcome outcome
    ) {
        return new AuditRecordRequest(
                context, "notification", action,
                AuditTarget.global("notification-dispatch", dispatch.dispatchId().value()),
                outcome, Map.of("status", dispatch.status().name(),
                "template", dispatch.templateKey().value()));
    }

    private static String deliveryKey(NotificationRecipient recipient, NotificationChannel channel) {
        return recipient.canonicalValue() + "|" + channel.value();
    }

    private static String deliveryIdempotencyKey(
            NotificationDispatch dispatch,
            NotificationDelivery delivery
    ) {
        return dispatch.idempotencyKey() + "|" + deliveryKey(delivery.recipient(), delivery.channel());
    }

    private static String fingerprint(SendNotificationRequest request) {
        StringBuilder value = new StringBuilder();
        appendPart(value, request.templateKey().value());
        request.recipients().stream()
                .sorted(java.util.Comparator.comparing(NotificationRecipient::canonicalValue))
                .forEach(recipient -> {
                    appendPart(value, recipient.type().value());
                    appendPart(value, recipient.recipientId());
                    appendMap(value, recipient.attributes());
                });
        appendMap(value, request.parameters());
        appendMap(value, request.attributes());
        return sha256(value.toString());
    }

    private static void appendMap(StringBuilder target, Map<String, String> values) {
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            appendPart(target, entry.getKey());
            appendPart(target, entry.getValue());
        });
    }

    private static void appendPart(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        String value = exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message.trim());
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }

    private static Map<NotificationChannel, NotificationChannelProvider> indexProviders(
            Collection<? extends NotificationChannelProvider> providers
    ) {
        Objects.requireNonNull(providers, "providers must not be null");
        return providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> Objects.requireNonNull(provider.channel(), "provider channel must not be null"),
                Function.identity(),
                (first, second) -> {
                    throw new IllegalArgumentException("duplicate notification provider: " + first.channel());
                }
        ));
    }
}
