package com.datausher.integration.compute.local;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface LocalSqlConnectionFactory {
    Connection open(String bindingId) throws SQLException;
}
