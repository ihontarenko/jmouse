package org.jmouse.jdbc.connection;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.support.AbstractConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * {@link ConnectionProvider} backed by a {@link DataSource}.
 */
public final class DataSourceConnectionProvider extends AbstractConnectionProvider {

    private final DataSource dataSource;

    public DataSourceConnectionProvider(DataSource dataSource) {
        this.dataSource = Contract.nonNull(dataSource, "dataSource");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
