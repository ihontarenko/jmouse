package org.jmouse.jdbc.connection.datasource;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.connection.datasource.support.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class ResolvableDataSource extends AbstractDataSource {

    private final DataSourceResolver resolver;
    private final DataSourceKey      lookupKey;

    public ResolvableDataSource(DataSourceResolver resolver, DataSourceKey lookupKey) {
        this.resolver = Verify.nonNull(resolver, "resolver");
        this.lookupKey = Verify.nonNull(lookupKey, "lookupKey");
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
        return resolver.resolve(Verify.notBlank(lookupKey.get(), "Specify data-source-key to resolve data-source"));
    }
}
