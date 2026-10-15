package org.jmouse.scheduler.jdbc;

import org.jmouse.scheduler.spi.TriggerStoreException;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The two tables {@link JdbcTriggerStore} needs, as a script per database.
 *
 * <p>The scripts live at {@code org/jmouse/scheduler/jdbc/schema-postgresql.sql} and
 * {@code schema-mysql.sql} on the classpath. A product with its own migration tool copies the script
 * into a migration of its own — the recommended route, because then the product's migration history
 * says when the tables appeared. {@link #create} runs the script directly for everyone else; every
 * statement is {@code IF NOT EXISTS}, so running it at every start is harmless.
 */
public final class JdbcTriggerSchema {

    /** The databases a script is written for. */
    public enum Dialect {

        /** PostgreSQL; H2 runs this script, and this query, too. */
        POSTGRESQL("schema-postgresql.sql", "SELECT CAST(EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) * 1000 AS BIGINT)"),

        /** MySQL 8 and MariaDB. */
        MYSQL("schema-mysql.sql", "SELECT CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS SIGNED)");

        private final String scriptName;
        private final String currentTimeQuery;

        Dialect(String scriptName, String currentTimeQuery) {
            this.scriptName = scriptName;
            this.currentTimeQuery = currentTimeQuery;
        }

        /** The classpath location of this dialect's script. */
        public String scriptLocation() {
            return "org/jmouse/scheduler/jdbc/" + scriptName;
        }

        /**
         * A query answering the database's current time as epoch milliseconds — the clock leases are
         * measured on, so the clocks of the machines running the runners never decide an expiry.
         */
        public String currentTimeQuery() {
            return currentTimeQuery;
        }
    }

    private JdbcTriggerSchema() {
    }

    /**
     * Creates the tables when they are missing, choosing the script from the database's product name.
     *
     * @param dataSource the database
     * @throws TriggerStoreException when the database is not one a script is written for, or a statement fails
     */
    public static void create(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            Dialect dialect = detect(connection.getMetaData().getDatabaseProductName());

            try (Statement statement = connection.createStatement()) {
                for (String sql : statements(dialect)) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException exception) {
            throw new TriggerStoreException("Could not create the jmouse-scheduler tables", exception);
        }
    }

    /**
     * @param databaseProductName {@link java.sql.DatabaseMetaData#getDatabaseProductName()}
     * @return the dialect whose script that database runs
     * @throws TriggerStoreException for a database no script is written for
     */
    public static Dialect detect(String databaseProductName) {
        String name = databaseProductName == null ? "" : databaseProductName.toLowerCase(Locale.ROOT);

        if (name.contains("mysql") || name.contains("mariadb")) {
            return Dialect.MYSQL;
        }

        if (name.contains("postgresql") || name.contains("h2")) {
            return Dialect.POSTGRESQL;
        }

        throw new TriggerStoreException("jmouse-scheduler has no schema script for '" + databaseProductName
                + "'; create the tables by hand from schema-postgresql.sql", null);
    }

    /**
     * @param dialect the database
     * @return the script's statements, comments removed
     */
    public static List<String> statements(Dialect dialect) {
        String script = readScript(dialect.scriptLocation());
        StringBuilder withoutComments = new StringBuilder();

        for (String line : script.split("\\R")) {
            if (!line.strip().startsWith("--")) {
                withoutComments.append(line).append('\n');
            }
        }

        List<String> statements = new ArrayList<>();

        for (String statement : withoutComments.toString().split(";")) {
            if (!statement.isBlank()) {
                statements.add(statement.strip());
            }
        }

        return statements;
    }

    private static String readScript(String location) {
        try (InputStream input = JdbcTriggerSchema.class.getClassLoader().getResourceAsStream(location)) {
            if (input == null) {
                throw new IllegalStateException("The schema script '" + location + "' is missing from the classpath.");
            }

            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the schema script '" + location + "'.", exception);
        }
    }
}
