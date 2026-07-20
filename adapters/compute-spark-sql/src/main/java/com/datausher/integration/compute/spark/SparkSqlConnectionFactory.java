package com.datausher.integration.compute.spark;

import com.datausher.integration.compute.api.ComputeJobRequest;

import java.sql.Connection;
import java.sql.SQLException;

public interface SparkSqlConnectionFactory {
    Connection open(ComputeJobRequest request) throws SQLException;
}
