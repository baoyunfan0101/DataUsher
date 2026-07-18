package com.datausher.integration.datasource.mysql;

import com.datausher.integration.datasource.api.DatasourceConnection;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface JdbcConnectionFactory {
    Connection open(DatasourceConnection connection) throws SQLException;
}
