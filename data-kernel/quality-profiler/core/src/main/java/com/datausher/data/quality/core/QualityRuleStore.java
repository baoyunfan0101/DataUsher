package com.datausher.data.quality.core;

import com.datausher.data.quality.api.QualityRule;
import com.datausher.data.quality.api.QualityRuleId;
import com.datausher.data.quality.api.QualityRuleStatus;
import com.datausher.data.quality.api.QualityRuleVersion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QualityRuleStore {
    void create(QualityRule rule, QualityRuleVersion version);

    void addVersion(
            QualityRule expected,
            QualityRule replacement,
            QualityRuleVersion version
    );

    void changeStatus(
            QualityRule expected,
            QualityRuleStatus status,
            Instant updatedAt
    );

    Optional<QualityRule> find(QualityRuleId ruleId);

    Optional<QualityRuleVersion> findVersion(QualityRuleId ruleId, long version);

    List<QualityRuleVersion> listVersions(QualityRuleId ruleId);
}
