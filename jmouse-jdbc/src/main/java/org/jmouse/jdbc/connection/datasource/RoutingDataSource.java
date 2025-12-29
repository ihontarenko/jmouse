package org.jmouse.jdbc.connection.datasource;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.datasource.support.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class RoutingDataSource extends AbstractDataSource {

    private final DataSourceResolver resolver;
    private final DataSourceKey      lookupKey;
    private final boolean            lenientFallback;
    private final String             defaultKey;

    public RoutingDataSource(
            DataSourceResolver resolver, DataSourceKey lookupKey, boolean lenientFallback, String defaultKey
    ) {
        this.resolver = Contract.nonNull(resolver, "resolver");
        this.lookupKey = Contract.nonNull(lookupKey, "lookupKey");
        this.lenientFallback = lenientFallback;
        this.defaultKey = defaultKey;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return resolveDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return resolveDataSource().getConnection(username, password);
    }

    private DataSource resolveDataSource() {
        String key = lookupKey.get();

        if (key == null || key.isBlank()) {
            if (defaultKey != null) {
                return resolver.resolve(defaultKey);
            }

            if (lenientFallback) {
                return resolver.resolve(key);
            }

            throw new IllegalStateException("No lookup key and no default data source configured");
        }

        try {
            return resolver.resolve(key);
        } catch (RuntimeException exception) {
            if (lenientFallback && defaultKey != null) {
                return resolver.resolve(defaultKey);
            }
            throw exception;
        }
    }
}
