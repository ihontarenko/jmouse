package org.jmouse.jdbc.connection.datasource.support;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.datasource.DataSourceResolver;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public final class RoutingDataSource extends AbstractDataSource {

    private final DataSourceResolver resolver;
    private final Supplier<String>   lookupKey;
    private final boolean            lenientFallback;
    private final String             defaultKey;

    public RoutingDataSource(
            DataSourceResolver resolver, Supplier<String> lookupKey, boolean lenientFallback, String defaultKey
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
