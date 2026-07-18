package com.datausher.integration.compute.api;

public final class ComputeCapabilities {
    public static final String JOB_EXECUTION = "compute.job.execute";
    public static final String JOB_CANCELLATION = "compute.job.cancel";
    public static final String JOB_LOGS = "compute.job.logs";
    public static final String JOB_RESULTS = "compute.job.results";
    public static final String SQL_EXECUTION = "compute.sql.execute";
    public static final String SQL_EXPLAIN = "compute.sql.explain";
    public static final String SCRIPT_EXECUTION = "compute.script.execute";

    private ComputeCapabilities() {
    }
}
