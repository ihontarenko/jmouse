package org.jmouse.jdbc.transaction;

import java.sql.Connection;
import java.util.Objects;

/** A bound connection and the datasource/provider resource that owns it. */
public final class JdbcResourceHolder {
    private final Connection connection;
    private final Object resourceKey;
    private boolean synchronizedWithTransaction;

    /** Legacy unkeyed holder for integrations managing a single resource. */
    public JdbcResourceHolder(Connection connection) {
        this(connection, null);
    }

    public JdbcResourceHolder(Connection connection, Object resourceKey) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.resourceKey = resourceKey;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean belongsTo(Object key) {
        return resourceKey == null || resourceKey == key;
    }

    public boolean isSynchronized() {
        return synchronizedWithTransaction;
    }

    public void setSynchronized(boolean synchronizedWithTransaction) {
        this.synchronizedWithTransaction = synchronizedWithTransaction;
    }
}
