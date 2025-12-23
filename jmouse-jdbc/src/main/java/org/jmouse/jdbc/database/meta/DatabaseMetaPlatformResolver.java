package org.jmouse.jdbc.database.meta;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.database.DatabaseInformation;
import org.jmouse.jdbc.database.DatabasePlatform;
import org.jmouse.jdbc.database.DatabasePlatformRegistry;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Resolves {@link DatabasePlatform} using JDBC {@link DatabaseMetaData}.
 *
 * <p>Bootstrap-time resolver: acquires a temporary connection and releases it via provider.</p>
 */
public final class DatabaseMetaPlatformResolver {

    private final ConnectionProvider       provider;
    private final DatabasePlatformRegistry registry;

    public DatabaseMetaPlatformResolver(ConnectionProvider provider, DatabasePlatformRegistry registry) {
        this.provider = Contract.nonNull(provider, "provider");
        this.registry = Contract.nonNull(registry, "registry");
    }

    public DatabasePlatform resolve() {
        Connection connection = null;
        try {
            connection = provider.getConnection();
            DatabaseMetaData metaData = connection.getMetaData();

            DatabaseInformation info = new DatabaseInformation(
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion(),
                    metaData.getDatabaseMajorVersion(),
                    metaData.getDatabaseMinorVersion()
            );

            return registry.resolve(info);
        } catch (SQLException e) {
            return registry.fallback();
        } finally {
            if (connection != null) {
                provider.release(connection);
            }
        }
    }
}
