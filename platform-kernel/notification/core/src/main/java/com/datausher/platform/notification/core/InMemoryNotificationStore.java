package com.datausher.platform.notification.core;

import com.datausher.platform.notification.api.NotificationDispatchId;
import com.datausher.platform.notification.api.NotificationTemplate;
import com.datausher.platform.notification.api.NotificationTemplateKey;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryNotificationStore implements NotificationStore {
    private final ConcurrentMap<String, NotificationTemplate> templates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StoredNotificationDispatch> dispatches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> idempotencyIndex = new ConcurrentHashMap<>();

    @Override
    public Optional<NotificationTemplate> findTemplate(NotificationTemplateKey templateKey, long version) {
        return Optional.ofNullable(templates.get(templateKey(templateKey, version)));
    }

    @Override
    public Optional<NotificationTemplate> findLatestTemplate(NotificationTemplateKey templateKey) {
        return templates.values().stream()
                .filter(template -> template.templateKey().equals(templateKey))
                .max(Comparator.comparingLong(NotificationTemplate::version));
    }

    @Override
    public List<NotificationTemplate> listTemplateVersions(NotificationTemplateKey templateKey) {
        return templates.values().stream()
                .filter(template -> template.templateKey().equals(templateKey))
                .sorted(Comparator.comparingLong(NotificationTemplate::version))
                .toList();
    }

    @Override
    public synchronized void createTemplate(NotificationTemplate template) {
        long latestVersion = findLatestTemplate(template.templateKey())
                .map(NotificationTemplate::version)
                .orElse(0L);
        if (template.version() != latestVersion + 1) {
            throw new IllegalStateException("template version changed concurrently: " + template.templateKey());
        }
        templates.put(templateKey(template.templateKey(), template.version()), template);
    }

    @Override
    public void deleteTemplate(NotificationTemplate template) {
        if (!templates.remove(templateKey(template.templateKey(), template.version()), template)) {
            throw new IllegalStateException("template changed before rollback: " + template.templateKey());
        }
    }

    @Override
    public Optional<StoredNotificationDispatch> findDispatch(NotificationDispatchId dispatchId) {
        return Optional.ofNullable(dispatches.get(dispatchId.value()));
    }

    @Override
    public Optional<StoredNotificationDispatch> findByIdempotencyKey(String idempotencyKey) {
        String dispatchId = idempotencyIndex.get(idempotencyKey);
        return dispatchId == null ? Optional.empty() : Optional.ofNullable(dispatches.get(dispatchId));
    }

    @Override
    public synchronized void createDispatch(StoredNotificationDispatch stored) {
        String dispatchId = stored.dispatch().dispatchId().value();
        String idempotencyKey = stored.dispatch().idempotencyKey();
        if (dispatches.containsKey(dispatchId)) {
            throw new IllegalStateException("notification dispatch already exists: " + dispatchId);
        }
        String existingId = idempotencyIndex.putIfAbsent(idempotencyKey, dispatchId);
        if (existingId != null) {
            throw new IllegalStateException("notification idempotency key already exists: " + idempotencyKey);
        }
        dispatches.put(dispatchId, stored);
    }

    @Override
    public synchronized void deleteDispatch(StoredNotificationDispatch stored) {
        String dispatchId = stored.dispatch().dispatchId().value();
        if (!dispatches.remove(dispatchId, stored)
                || !idempotencyIndex.remove(stored.dispatch().idempotencyKey(), dispatchId)) {
            throw new IllegalStateException("notification dispatch changed before rollback: " + dispatchId);
        }
    }

    @Override
    public void updateDispatch(StoredNotificationDispatch expected, StoredNotificationDispatch replacement) {
        if (!expected.dispatch().dispatchId().equals(replacement.dispatch().dispatchId())
                || !expected.dispatch().idempotencyKey().equals(replacement.dispatch().idempotencyKey())) {
            throw new IllegalArgumentException("notification dispatch identity must not change");
        }
        if (!dispatches.replace(expected.dispatch().dispatchId().value(), expected, replacement)) {
            throw new IllegalStateException(
                    "notification dispatch changed concurrently: " + expected.dispatch().dispatchId());
        }
    }

    private static String templateKey(NotificationTemplateKey templateKey, long version) {
        return templateKey.value() + "@" + version;
    }
}
