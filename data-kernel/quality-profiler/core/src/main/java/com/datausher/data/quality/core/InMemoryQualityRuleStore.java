package com.datausher.data.quality.core;

import com.datausher.data.quality.api.QualityRule;
import com.datausher.data.quality.api.QualityRuleId;
import com.datausher.data.quality.api.QualityRuleStatus;
import com.datausher.data.quality.api.QualityRuleVersion;
import com.datausher.platform.shared.concurrent.RevisionConflictException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryQualityRuleStore implements QualityRuleStore {
    private final Map<QualityRuleId, QualityRule> rules = new HashMap<>();
    private final Map<QualityRuleId, List<QualityRuleVersion>> versions = new HashMap<>();

    @Override
    public synchronized void create(QualityRule rule, QualityRuleVersion version) {
        if (rules.putIfAbsent(rule.ruleId(), rule) != null) {
            throw new IllegalStateException("quality rule already exists: " + rule.ruleId().value());
        }
        versions.put(rule.ruleId(), List.of(version));
    }

    @Override
    public synchronized void addVersion(
            QualityRule expected,
            QualityRule replacement,
            QualityRuleVersion version
    ) {
        replace(expected, replacement);
        List<QualityRuleVersion> next = new ArrayList<>(
                versions.getOrDefault(expected.ruleId(), List.of()));
        next.add(version);
        versions.put(expected.ruleId(), List.copyOf(next));
    }

    @Override
    public synchronized void changeStatus(
            QualityRule expected,
            QualityRuleStatus status,
            Instant updatedAt
    ) {
        QualityRule replacement = new QualityRule(
                expected.ruleId(), expected.latestVersion(), status, expected.attributes(),
                expected.createdAt(), updatedAt, expected.revision() + 1);
        replace(expected, replacement);
    }

    @Override
    public synchronized Optional<QualityRule> find(QualityRuleId ruleId) {
        return Optional.ofNullable(rules.get(ruleId));
    }

    @Override
    public synchronized Optional<QualityRuleVersion> findVersion(
            QualityRuleId ruleId,
            long version
    ) {
        return versions.getOrDefault(ruleId, List.of()).stream()
                .filter(candidate -> candidate.version() == version).findFirst();
    }

    @Override
    public synchronized List<QualityRuleVersion> listVersions(QualityRuleId ruleId) {
        return List.copyOf(versions.getOrDefault(ruleId, List.of()));
    }

    private void replace(QualityRule expected, QualityRule replacement) {
        if (!rules.replace(expected.ruleId(), expected, replacement)) {
            QualityRule actual = rules.get(expected.ruleId());
            if (actual != null) {
                throw new RevisionConflictException(
                        "quality-rule", expected.ruleId().value(),
                        expected.revision(), actual.revision());
            }
            throw new IllegalStateException("quality rule no longer exists");
        }
    }
}
