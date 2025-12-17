package org.jmouse.jdbc.dialect.meta;

import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.dialect.DialectResolver;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves dialect id using JDBC DatabaseMetaData (product name/version).
 *
 * <p>Designed for bootstrap stage: acquires a temporary connection and closes it via provider.</p>
 */
public final class DatabaseMetaDialectResolver implements DialectResolver {

    private final ConnectionProvider provider;
    private final DialectCatalog catalog;
    private final String fallbackDialectId;

    public DatabaseMetaDialectResolver(ConnectionProvider provider, DialectCatalog catalog, String fallbackDialectId) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.fallbackDialectId = Objects.requireNonNull(fallbackDialectId, "fallbackDialectId");
    }

    @Override
    public String resolveDialectId() {
        Connection c = null;
        try {
            c = provider.getConnection();
            DatabaseMetaData md = c.getMetaData();

            DatabaseInfo info = new DatabaseInfo(
                    md.getDatabaseProductName(),
                    md.getDatabaseMajorVersion(),
                    md.getDatabaseMinorVersion()
            );

            Optional<String> id = catalog.resolve(info);
            return id.orElse(fallbackDialectId);

        } catch (SQLException e) {
            // Bootstrap-time policy: fallback if metadata fails.
            return fallbackDialectId;

        } finally {
            if (c != null) {
                try {
                    provider.release(c);
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
