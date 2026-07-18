package com.datausher.execution.sql.api;

import com.datausher.execution.api.ExecutionValue;
import com.datausher.execution.api.ExecutionWorkload;
import com.datausher.execution.api.ExecutionWorkloadType;

import java.util.Map;

public final class SqlWorkloads {
    public static final ExecutionWorkloadType TYPE = new ExecutionWorkloadType("sql");

    private SqlWorkloads() {
    }

    public static ExecutionWorkload statement(
            String statement,
            Map<String, ExecutionValue> parameters,
            Map<String, String> options
    ) {
        return new ExecutionWorkload(TYPE, statement, parameters, options);
    }
}
