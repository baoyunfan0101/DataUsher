package com.datausher.platform.notification.core;

import com.datausher.platform.notification.api.NotificationDispatchId;
import com.datausher.platform.notification.api.NotificationTemplate;
import com.datausher.platform.notification.api.NotificationTemplateKey;

import java.util.List;
import java.util.Optional;

public interface NotificationStore {
    Optional<NotificationTemplate> findTemplate(NotificationTemplateKey templateKey, long version);

    Optional<NotificationTemplate> findLatestTemplate(NotificationTemplateKey templateKey);

    List<NotificationTemplate> listTemplateVersions(NotificationTemplateKey templateKey);

    void createTemplate(NotificationTemplate template);

    void deleteTemplate(NotificationTemplate template);

    Optional<StoredNotificationDispatch> findDispatch(NotificationDispatchId dispatchId);

    Optional<StoredNotificationDispatch> findByIdempotencyKey(String idempotencyKey);

    void createDispatch(StoredNotificationDispatch dispatch);

    void deleteDispatch(StoredNotificationDispatch dispatch);

    void updateDispatch(StoredNotificationDispatch expected, StoredNotificationDispatch replacement);
}
