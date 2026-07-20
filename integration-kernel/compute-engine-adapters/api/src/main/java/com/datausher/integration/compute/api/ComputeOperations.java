package com.datausher.integration.compute.api;

import com.datausher.integration.runtime.api.AdapterOperation;

public final class ComputeOperations {
    public static final AdapterOperation SUBMIT_JOB = AdapterOperation.of(
            "compute.job.submit", ComputeCapabilities.JOB_EXECUTION, true);
    public static final AdapterOperation READ_STATUS = AdapterOperation.of(
            "compute.job.status", ComputeCapabilities.JOB_EXECUTION, false);
    public static final AdapterOperation CANCEL_JOB = AdapterOperation.of(
            "compute.job.cancel", ComputeCapabilities.JOB_CANCELLATION, true);
    public static final AdapterOperation READ_LOGS = AdapterOperation.of(
            "compute.job.logs.read", ComputeCapabilities.JOB_LOGS, false);
    public static final AdapterOperation READ_RESULT = AdapterOperation.of(
            "compute.job.result.read", ComputeCapabilities.JOB_RESULTS, false);
    public static final AdapterOperation EXPLAIN_SQL = AdapterOperation.of(
            "compute.sql.explain", ComputeCapabilities.SQL_EXPLAIN, false);

    private ComputeOperations() {
    }
}
