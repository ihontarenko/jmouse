package org.jmouse.jdbc;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.core.JdbcOperations;
import org.jmouse.jdbc.database.DatabasePlatform;

/**
 * Application-level JDBC entrypoint.
 *
 * <p>Exposes high-level {@link JdbcOperations} and resolved {@link DatabasePlatform}.</p>
 */
public record JdbcClient(JdbcOperations jdbc, DatabasePlatform platform) {

    public JdbcClient(JdbcOperations jdbc, DatabasePlatform platform) {
        this.jdbc = Contract.nonNull(jdbc, "jdbc");
        this.platform = Contract.nonNull(platform, "platform");
    }

}