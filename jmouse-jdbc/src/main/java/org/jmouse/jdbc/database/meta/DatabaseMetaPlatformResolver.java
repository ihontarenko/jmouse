package org.jmouse.jdbc.database.meta;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.database.DatabaseInformation;
import org.jmouse.jdbc.database.DatabasePlatform;
import org.jmouse.jdbc.database.DatabasePlatformRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Resolves {@link DatabasePlatform} using JDBC {@link DatabaseMetaData}.
 *
 * <p>Bootstrap-time resolver: acquires a temporary connection and releases it via provider.</p>
 */
public final class DatabaseMetaPlatformResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMetaPlatformResolver.class);

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

            DatabasePlatform platform = registry.resolve(info);

            LOGGER.info("Database Platform Detected: {}", platform.displayName());

            return platform;
        } catch (SQLException e) {
            return registry.fallback();
        } finally {
            if (connection != null) {
                provider.release(connection);
            }
        }
    }
}
