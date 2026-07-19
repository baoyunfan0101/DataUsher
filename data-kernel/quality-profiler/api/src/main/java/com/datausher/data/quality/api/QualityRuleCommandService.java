package com.datausher.data.quality.api;

public interface QualityRuleCommandService {
    QualityRule create(CreateQualityRuleRequest request);

    QualityRuleVersion createVersion(CreateQualityRuleVersionRequest request);

    QualityRule changeStatus(ChangeQualityRuleStatusRequest request);
}
