package org.jmouse.jdbc;

import java.sql.Connection;

/**
 * 🧵 Thread-local holder for a JDBC {@link Connection}.
 * If present, all low-level operations should prefer it.
 */
public interface ConnectionBinding {

    void bind(Connection connection);

    void unbind();

    Connection currentConnection();

}