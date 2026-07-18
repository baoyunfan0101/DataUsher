package com.datausher.platform.notification.core;

import com.datausher.platform.audit.core.CompensatingAuditedCommandExecutor;
import com.datausher.platform.audit.core.DefaultAuditService;
import com.datausher.platform.audit.core.InMemoryAuditEventStore;
import com.datausher.platform.notification.api.NotificationChannel;
import com.datausher.platform.notification.api.NotificationChannelProvider;
import com.datausher.platform.notification.api.NotificationChannelResult;
import com.datausher.platform.notification.api.NotificationContent;
import com.datausher.platform.notification.api.NotificationDispatchStatus;
import com.datausher.platform.notification.api.NotificationRecipient;
import com.datausher.platform.notification.api.NotificationRecipientType;
import com.datausher.platform.notification.api.NotificationRoute;
import com.datausher.platform.notification.api.NotificationTemplateKey;
import com.datausher.platform.notification.api.PublishNotificationTemplateRequest;
import com.datausher.platform.notification.api.RetryNotificationRequest;
import com.datausher.platform.notification.api.SendNotificationRequest;
import com.datausher.platform.shared.context.RequestContext;
import com.datausher.platform.shared.id.core.UuidIdGenerator;
import com.datausher.platform.shared.time.core.SystemClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultNotificationServiceTest {
    @Test
    void rendersDispatchesAndRetriesFailedDeliveries() {
        AtomicInteger attempts = new AtomicInteger();
        NotificationChannel channel = new NotificationChannel("chat");
        NotificationChannelProvider provider = new NotificationChannelProvider() {
            @Override
            public NotificationChannel channel() {
                return channel;
            }

            @Override
            public NotificationChannelResult deliver(
                    com.datausher.platform.notification.api.NotificationEnvelope envelope
            ) {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("temporary failure");
                }
                assertEquals("Approved workflow daily", envelope.content().body());
                return new NotificationChannelResult("message-1", Map.of());
            }
        };
        DefaultNotificationService service = service(provider);
        RequestContext context = RequestContext.system("request-1", Instant.now());
        NotificationTemplateKey key = new NotificationTemplateKey("approval-completed");
        service.publishTemplate(new PublishNotificationTemplateRequest(
                key, "Approval Completed",
                List.of(new NotificationRoute(channel,
                        new NotificationContent("text/plain", "Approved", "Approved workflow ${name}", Map.of()))),
                Map.of(), context));

        var dispatch = service.send(new SendNotificationRequest(
                key, List.of(new NotificationRecipient(NotificationRecipientType.SUBJECT, "owner-1", Map.of())),
                Map.of("name", "daily"), "approval-1", Map.of(), context));
        assertEquals(NotificationDispatchStatus.FAILED, dispatch.status());

        dispatch = service.retry(new RetryNotificationRequest(dispatch.dispatchId(), context));
        assertEquals(NotificationDispatchStatus.SUCCEEDED, dispatch.status());
        assertEquals(2, attempts.get());
    }

    @Test
    void rejectsIdempotencyKeyReuseForDifferentRequests() {
        NotificationChannel channel = new NotificationChannel("chat");
        DefaultNotificationService service = service(new SuccessfulProvider(channel));
        RequestContext context = RequestContext.system("request-1", Instant.now());
        NotificationTemplateKey key = new NotificationTemplateKey("approval-completed");
        service.publishTemplate(new PublishNotificationTemplateRequest(
                key, "Approval Completed",
                List.of(new NotificationRoute(channel,
                        new NotificationContent("text/plain", "Approved", "Status ${status}", Map.of()))),
                Map.of(), context));
        NotificationRecipient recipient = new NotificationRecipient(
                NotificationRecipientType.SUBJECT, "owner-1", Map.of());
        service.send(new SendNotificationRequest(
                key, List.of(recipient), Map.of("status", "approved"), "same-key", Map.of(), context));

        assertThrows(IllegalStateException.class, () -> service.send(new SendNotificationRequest(
                key, List.of(recipient), Map.of("status", "rejected"), "same-key", Map.of(), context)));
    }

    private static DefaultNotificationService service(NotificationChannelProvider provider) {
        var clock = new SystemClock();
        var ids = new UuidIdGenerator();
        var audit = new DefaultAuditService(new InMemoryAuditEventStore(), ids, clock);
        return new DefaultNotificationService(
                new InMemoryNotificationStore(), new StrictNotificationTemplateRenderer(), List.of(provider),
                ids, clock, new CompensatingAuditedCommandExecutor(audit), audit);
    }

    private record SuccessfulProvider(NotificationChannel channel) implements NotificationChannelProvider {
        @Override
        public NotificationChannelResult deliver(
                com.datausher.platform.notification.api.NotificationEnvelope envelope
        ) {
            return new NotificationChannelResult("message-1", Map.of());
        }
    }
}
