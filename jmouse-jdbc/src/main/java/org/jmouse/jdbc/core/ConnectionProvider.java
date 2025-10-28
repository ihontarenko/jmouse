package org.jmouse.jdbc.core;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 🔌 Lightweight handle for obtaining a JDBC {@link Connection}.
 */
public interface ConnectionProvider {
    Connection acquire() throws SQLException;
}
