package com.datausher.data.quality.api;

public final class DataQualityEvents {
    public static final String PROFILE_JOB_STATE_CHANGED =
            "quality-profiler.profile-job-state-changed";
    public static final String QUALITY_RULE_CREATED = "quality-profiler.rule-created";
    public static final String QUALITY_RULE_VERSION_CREATED =
            "quality-profiler.rule-version-created";
    public static final String QUALITY_RULE_STATUS_CHANGED =
            "quality-profiler.rule-status-changed";
    public static final String QUALITY_CHECK_STATE_CHANGED =
            "quality-profiler.check-state-changed";
    public static final String DATA_ANOMALY_DETECTED =
            "quality-profiler.data-anomaly-detected";

    private DataQualityEvents() {
    }
}
