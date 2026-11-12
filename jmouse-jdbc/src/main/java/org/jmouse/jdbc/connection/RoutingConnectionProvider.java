package org.jmouse.jdbc.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import org.jmouse.core.Verify;
import org.jmouse.jdbc.connection.datasource.DataSourceResolver;
import org.jmouse.jdbc.connection.support.AbstractConnectionProvider;

public final class RoutingConnectionProvider extends AbstractConnectionProvider {

    private final DataSourceResolver resolver;
    private final Supplier<String> lookupKey;

    public RoutingConnectionProvider(DataSourceResolver resolver, Supplier<String> lookupKey) {
        this.resolver = Verify.nonNull(resolver, "resolver");
        this.lookupKey = Verify.nonNull(lookupKey, "lookupKey");
    }

    @Override
    public Object getResourceKey() {
        // The routing provider is the logical resource. A transaction pins its first connection.
        return this;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return resolver.resolve(lookupKey.get()).getConnection();
    }
}
