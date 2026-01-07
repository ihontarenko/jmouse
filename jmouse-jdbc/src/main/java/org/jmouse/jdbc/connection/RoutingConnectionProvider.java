package org.jmouse.jdbc.connection;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.connection.datasource.DataSourceResolver;
import org.jmouse.jdbc.connection.support.AbstractConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public final class RoutingConnectionProvider extends AbstractConnectionProvider {

    private final DataSourceResolver resolver;
    private final Supplier<String>   lookupKey;

    public RoutingConnectionProvider(DataSourceResolver resolver, Supplier<String> lookupKey) {
        this.resolver = Verify.nonNull(resolver, "resolver");
        this.lookupKey = Verify.nonNull(lookupKey, "lookupKey");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return resolver.resolve(lookupKey.get()).getConnection();
    }

}
