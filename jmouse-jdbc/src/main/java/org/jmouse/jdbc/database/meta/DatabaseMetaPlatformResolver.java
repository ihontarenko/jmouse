package org.jmouse.jdbc.database.meta;

import org.jmouse.core.Verify;
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
 * Resolves the active {@link DatabasePlatform} using JDBC {@link DatabaseMetaData}.
 * <p>
 * {@code DatabaseMetaPlatformResolver} is a bootstrap-time component responsible for:
 * <ul>
 *     <li>opening a temporary JDBC {@link Connection}</li>
 *     <li>extracting vendor and version information via {@link DatabaseMetaData}</li>
 *     <li>resolving an appropriate {@link DatabasePlatform} from the registry</li>
 *     <li>releasing the connection back to the {@link ConnectionProvider}</li>
 * </ul>
 *
 * <p>
 * This resolver is typically invoked once during application startup and the
 * resulting {@link DatabasePlatform} is reused throughout the application.
 *
 * <h3>Failure behavior</h3>
 * <p>
 * If JDBC metadata cannot be obtained (for example, due to a {@link SQLException}),
 * the registry fallback platform is returned.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * DatabaseMetaPlatformResolver resolver =
 *     new DatabaseMetaPlatformResolver(connectionProvider, platformRegistry);
 *
 * DatabasePlatform platform = resolver.resolve();
 * }</pre>
 *
 * @author jMouse
 */
public final class DatabaseMetaPlatformResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMetaPlatformResolver.class);

    /**
     * Connection valueProvider used to obtain a temporary JDBC connection.
     */
    private final ConnectionProvider       provider;

    /**
     * Registry responsible for resolving database platforms.
     */
    private final DatabasePlatformRegistry registry;

    /**
     * Creates a new resolver instance.
     *
     * @param provider JDBC connection valueProvider
     * @param registry database platform registry
     */
    public DatabaseMetaPlatformResolver(ConnectionProvider provider, DatabasePlatformRegistry registry) {
        this.provider = Verify.nonNull(provider, "valueProvider");
        this.registry = Verify.nonNull(registry, "registry");
    }

    /**
     * Resolves the {@link DatabasePlatform} for the current database.
     * <p>
     * This method:
     * <ol>
     *     <li>Obtains a JDBC connection</li>
     *     <li>Reads {@link DatabaseMetaData}</li>
     *     <li>Builds a {@link DatabaseInformation} descriptor</li>
     *     <li>Delegates resolution to {@link DatabasePlatformRegistry}</li>
     * </ol>
     *
     * <p>
     * The connection is always released back to the valueProvider.
     *
     * @return resolved database platform (never {@code null})
     */
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
