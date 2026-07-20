package com.datausher.data.quality.api;

import java.util.List;
import java.util.Optional;

public interface QualityRuleQueryService {
    Optional<QualityRule> findRule(QualityRuleId ruleId);

    Optional<QualityRuleVersion> findVersion(QualityRuleId ruleId, long version);

    Optional<QualityRuleVersion> findLatestVersion(QualityRuleId ruleId);

    List<QualityRuleVersion> listVersions(QualityRuleId ruleId);
}
