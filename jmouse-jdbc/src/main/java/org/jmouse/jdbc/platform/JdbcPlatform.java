package org.jmouse.jdbc.platform;

import org.jmouse.jdbc.connection.ConnectionProvider;

/**
 * JDBC platform adapter.
 *
 * <p>Represents a concrete runtime integration strategy (DriverManager, DataSource, pool, JTA).
 * Must stay decoupled from core JDBC execution logic.</p>
 */
public interface JdbcPlatform {

    /**
     * Provides a raw connection provider for this platform.
     *
     * <p>Bootstrap is responsible for applying transaction-aware wrapping.</p>
     */
    ConnectionProvider connectionProvider();

    /**
     * Provides dialect discovery inputs for this platform (config + metadata access strategy).
     */
    DialectInputs dialectInputs();

    /**
     * Platform runtime capabilities (used for feature gating).
     */
    default PlatformCapabilities capabilities() {
        return PlatformCapabilities.defaults();
    }
}
