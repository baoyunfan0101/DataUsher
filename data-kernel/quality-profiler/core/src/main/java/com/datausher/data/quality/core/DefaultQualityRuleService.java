package com.datausher.data.quality.core;

import com.datausher.data.quality.api.ChangeQualityRuleStatusRequest;
import com.datausher.data.quality.api.CreateQualityRuleRequest;
import com.datausher.data.quality.api.CreateQualityRuleVersionRequest;
import com.datausher.data.quality.api.QualityRule;
import com.datausher.data.quality.api.QualityRuleCommandService;
import com.datausher.data.quality.api.QualityRuleId;
import com.datausher.data.quality.api.QualityRuleQueryService;
import com.datausher.data.quality.api.QualityRuleStatus;
import com.datausher.data.quality.api.QualityRuleCreatedEvent;
import com.datausher.data.quality.api.QualityRuleStatusChangedEvent;
import com.datausher.data.quality.api.QualityRuleVersion;
import com.datausher.data.quality.api.QualityRuleVersionCreatedEvent;
import com.datausher.platform.shared.concurrent.RevisionConflictException;
import com.datausher.platform.shared.event.DomainEventPublisher;
import com.datausher.platform.shared.id.IdGenerationRequest;
import com.datausher.platform.shared.id.IdGenerator;
import com.datausher.platform.shared.time.Clock;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DefaultQualityRuleService
        implements QualityRuleCommandService, QualityRuleQueryService {
    private final QualityRuleStore store;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final DomainEventPublisher eventPublisher;

    public DefaultQualityRuleService(
            QualityRuleStore store,
            Clock clock,
            IdGenerator idGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");
    }

    @Override
    public QualityRule create(CreateQualityRuleRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant now = clock.now();
        QualityRule rule = new QualityRule(
                request.ruleId(), 1, QualityRuleStatus.ACTIVE, request.attributes(),
                now, now, 1);
        QualityRuleVersion version = new QualityRuleVersion(
                request.ruleId(), 1, request.specification(), now,
                request.requestContext().actor().actorId());
        store.create(rule, version);
        eventPublisher.publish(new QualityRuleCreatedEvent(
                nextEventId(), now, request.requestContext(), rule, version));
        return rule;
    }

    @Override
    public QualityRuleVersion createVersion(CreateQualityRuleVersionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        QualityRule current = requireRule(request.ruleId());
        requireRevision(current, request.expectedRevision());
        if (current.status() == QualityRuleStatus.ARCHIVED) {
            throw new IllegalStateException("archived quality rule cannot be versioned");
        }
        Instant now = clock.now();
        long versionNumber = current.latestVersion() + 1;
        QualityRuleVersion version = new QualityRuleVersion(
                current.ruleId(), versionNumber, request.specification(), now,
                request.requestContext().actor().actorId());
        QualityRule replacement = new QualityRule(
                current.ruleId(), versionNumber, current.status(), current.attributes(),
                current.createdAt(), now, current.revision() + 1);
        store.addVersion(current, replacement, version);
        eventPublisher.publish(new QualityRuleVersionCreatedEvent(
                nextEventId(), now, request.requestContext(), version));
        return version;
    }

    @Override
    public QualityRule changeStatus(ChangeQualityRuleStatusRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        QualityRule current = requireRule(request.ruleId());
        requireRevision(current, request.expectedRevision());
        if (current.status() == request.status()) {
            return current;
        }
        if (current.status() == QualityRuleStatus.ARCHIVED) {
            throw new IllegalStateException("archived quality rule status cannot change");
        }
        store.changeStatus(current, request.status(), clock.now());
        QualityRule changed = requireRule(request.ruleId());
        eventPublisher.publish(new QualityRuleStatusChangedEvent(
                nextEventId(), changed.updatedAt(), request.requestContext(),
                current.status(), changed));
        return changed;
    }

    @Override
    public Optional<QualityRule> findRule(QualityRuleId ruleId) {
        return store.find(Objects.requireNonNull(ruleId, "ruleId must not be null"));
    }

    @Override
    public Optional<QualityRuleVersion> findVersion(QualityRuleId ruleId, long version) {
        return store.findVersion(
                Objects.requireNonNull(ruleId, "ruleId must not be null"), version);
    }

    @Override
    public Optional<QualityRuleVersion> findLatestVersion(QualityRuleId ruleId) {
        return store.find(Objects.requireNonNull(ruleId, "ruleId must not be null"))
                .flatMap(rule -> store.findVersion(ruleId, rule.latestVersion()));
    }

    @Override
    public List<QualityRuleVersion> listVersions(QualityRuleId ruleId) {
        return store.listVersions(Objects.requireNonNull(ruleId, "ruleId must not be null"));
    }

    private QualityRule requireRule(QualityRuleId ruleId) {
        return store.find(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("quality rule does not exist"));
    }

    private static void requireRevision(QualityRule current, long expectedRevision) {
        if (current.revision() != expectedRevision) {
            throw new RevisionConflictException(
                    "quality-rule", current.ruleId().value(),
                    expectedRevision, current.revision());
        }
    }

    private String nextEventId() {
        return idGenerator.nextIdValue(
                IdGenerationRequest.of("quality-profiler", "domain-event"));
    }
}
