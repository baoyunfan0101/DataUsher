package com.datausher.integration.datasource.jdbc;

import com.datausher.integration.datasource.api.DatasourceConnection;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcRelationalConnectionFactory {
    Connection open(DatasourceConnection connection) throws SQLException;
}
