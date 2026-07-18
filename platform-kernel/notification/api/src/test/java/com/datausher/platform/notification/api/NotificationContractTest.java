package com.datausher.platform.notification.api;

import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationContractTest {
    @Test
    void sendRequestDoesNotExposeDeliveryChannels() {
        SendNotificationRequest request = new SendNotificationRequest(
                new NotificationTemplateKey("approval-completed"),
                List.of(new NotificationRecipient(
                        NotificationRecipientType.SUBJECT, "user:owner-1", Map.of())),
                Map.of("status", "approved"),
                "approval-1",
                Map.of(),
                RequestContext.system("request-1", Instant.parse("2026-07-18T00:00:00Z"))
        );

        assertEquals("approval-completed", request.templateKey().value());
    }

    @Test
    void templateRoutesRequireUniqueChannels() {
        NotificationRoute route = new NotificationRoute(
                new NotificationChannel("chat"),
                new NotificationContent("text/plain", "Subject", "Body", Map.of()));

        assertThrows(IllegalArgumentException.class, () -> new PublishNotificationTemplateRequest(
                new NotificationTemplateKey("approval-completed"), "Approval Completed",
                List.of(route, route), Map.of(),
                RequestContext.system("request-1", Instant.parse("2026-07-18T00:00:00Z"))));
    }

    @Test
    void deliveredOutcomeRequiresAnExplicitReceiptTime() {
        NotificationRecipient recipient = new NotificationRecipient(
                NotificationRecipientType.SUBJECT, "user:owner-1", Map.of());
        NotificationChannel channel = new NotificationChannel("chat");
        Instant attemptedAt = Instant.parse("2026-07-18T00:00:00Z");

        NotificationDelivery accepted = new NotificationDelivery(
                recipient, channel, NotificationDeliveryStatus.ACCEPTED,
                1, attemptedAt, "message-1", null, "");

        assertEquals(NotificationDeliveryStatus.ACCEPTED, accepted.status());
        assertThrows(IllegalArgumentException.class, () -> new NotificationDelivery(
                recipient, channel, NotificationDeliveryStatus.DELIVERED,
                1, attemptedAt, "message-1", null, ""));
    }
}
