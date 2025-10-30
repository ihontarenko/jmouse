package org.jmouse.jdbc._wip.core;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 🔌 Lightweight handle for obtaining a JDBC {@link Connection}.
 */
public interface ConnectionProvider {
    Connection acquire() throws SQLException;
}
