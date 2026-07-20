package com.datausher.data.quality.api;

import com.datausher.execution.api.ExecutionAccountId;
import com.datausher.execution.api.ExecutionQueueId;
import com.datausher.platform.shared.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QualityProfilerContractTest {
    @Test
    void keepsTargetsMetricsRulesAndSeverityOpenForExtension() {
        assertEquals("feature-view", new DataTargetType("feature-view").value());
        assertEquals("entropy", new ProfileMetricType("entropy").value());
        assertEquals("freshness", new QualityRuleType("freshness").value());
        assertEquals("blocking", new QualitySeverity("blocking").value());
    }

    @Test
    void rejectsDuplicateProfileMetrics() {
        ProfileMetricSpec metric = new ProfileMetricSpec(
                ProfileMetricType.NULL_RATIO, Optional.of("customer_id"), Map.of());

        assertThrows(IllegalArgumentException.class, () -> new StartProfileJobRequest(
                target(), List.of(metric, metric), policy(), "profile-1", Map.of(), context()));
    }

    @Test
    void pinsQualityChecksToImmutableRuleVersions() {
        QualityRuleRef version = new QualityRuleRef(new QualityRuleId("not-null-id"), 3);
        StartQualityCheckRequest request = new StartQualityCheckRequest(
                List.of(version), policy(), "check-1", Map.of(), context());

        assertEquals(3, request.rules().getFirst().version());
    }

    private static DataTargetRef target() {
        return new DataTargetRef(DataTargetType.TABLE, "table-orders", Map.of());
    }

    private static DataExecutionPolicy policy() {
        return new DataExecutionPolicy(
                new ExecutionQueueId("default"), new ExecutionAccountId("local"),
                100, Map.of());
    }

    private static RequestContext context() {
        return RequestContext.system("request-1", Instant.EPOCH);
    }
}
