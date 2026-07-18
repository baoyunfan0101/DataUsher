package com.datausher.execution.sql.api;

public interface SqlExplainService {
    SqlExplainPlan explain(SqlExplainRequest request);
}
