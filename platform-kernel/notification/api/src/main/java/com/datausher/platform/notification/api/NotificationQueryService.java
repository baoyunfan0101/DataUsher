package com.datausher.platform.notification.api;

import java.util.List;
import java.util.Optional;

public interface NotificationQueryService {
    Optional<NotificationTemplate> findTemplate(NotificationTemplateKey templateKey, long version);

    Optional<NotificationTemplate> findLatestTemplate(NotificationTemplateKey templateKey);

    List<NotificationTemplate> listTemplateVersions(NotificationTemplateKey templateKey);

    Optional<NotificationDispatch> findDispatch(NotificationDispatchId dispatchId);

    Optional<NotificationDispatch> findByIdempotencyKey(String idempotencyKey);
}
