package org.jmouse.query.sql;

import org.jmouse.jdbc.dialect.Dialect;
import org.jmouse.jdbc.dialect.Dialects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * How a moment leaves the compiler: as an {@link OffsetDateTime}, which every supported driver binds — never as
 * the {@link Instant} {@code now()} is, which the PostgreSQL driver refuses.
 *
 * <p>The database half runs only when an address is given — {@code JMOUSE_TEST_MYSQL_URL} and
 * {@code JMOUSE_TEST_POSTGRESQL_URL}, each a JDBC URL carrying its own {@code user} and {@code password}. It
 * needs nothing but the right to create a temporary table.</p>
 */
class MomentBindingTest {

    private static final String SOURCES = """
            source events {
              from      moment_probe as e key id
              attribute event.happened from happened_at temporal in column
            }
            """;

    private static final String RECENT = "event.happened > now() - days(7)";
    private static final String BEFORE = "event.happened < cutoff";

    @Test
    @DisplayName("now() and a supplied Instant are bound as OffsetDateTime in UTC, for every dialect")
    void momentsAreBoundAsOffsetDateTime() {
        Instant cutoff = Instant.parse("2026-01-01T00:00:00Z");

        for (String product : List.of("MySQL", "PostgreSQL")) {
            QueryEngine engine = engine(Dialects.of(product));

            Fragment recent = engine.compileFilter("events", RECENT);
            Fragment before = engine.compileFilter("events", BEFORE, Map.of("cutoff", cutoff));

            assertFalse(recent.parameters().stream().anyMatch(Instant.class::isInstance), product + ": " + recent);

            OffsetDateTime now = recent.parameters().stream()
                    .filter(OffsetDateTime.class::isInstance)
                    .map(OffsetDateTime.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertEquals(ZoneOffset.UTC, now.getOffset(), product);
            assertEquals(List.of(cutoff.atOffset(ZoneOffset.UTC)), before.parameters(), product);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JMOUSE_TEST_POSTGRESQL_URL", matches = ".+")
    @DisplayName("PostgreSQL runs a filter on now() and on a supplied Instant")
    void postgresqlBindsMoments() throws SQLException {
        answersAgainst(System.getenv("JMOUSE_TEST_POSTGRESQL_URL"),
                "CREATE TEMPORARY TABLE moment_probe (id INTEGER PRIMARY KEY, happened_at TIMESTAMPTZ NOT NULL)",
                "INSERT INTO moment_probe VALUES (1, CURRENT_TIMESTAMP - INTERVAL '1 day'), "
                        + "(2, CURRENT_TIMESTAMP - INTERVAL '30 day')");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JMOUSE_TEST_MYSQL_URL", matches = ".+")
    @DisplayName("MySQL runs a filter on now() and on a supplied Instant")
    void mysqlBindsMoments() throws SQLException {
        answersAgainst(System.getenv("JMOUSE_TEST_MYSQL_URL"),
                "CREATE TEMPORARY TABLE moment_probe (id INTEGER PRIMARY KEY, happened_at DATETIME(6) NOT NULL)",
                "INSERT INTO moment_probe VALUES (1, CURRENT_TIMESTAMP(6) - INTERVAL 1 DAY), "
                        + "(2, CURRENT_TIMESTAMP(6) - INTERVAL 30 DAY)");
    }

    /**
     * One row a day old, one a month old: the last week holds the first, and a cutoff ten days back has only
     * the second before it. A day's margin either side keeps a driver's time-zone setting out of the answer.
     */
    private static void answersAgainst(String url, String createTable, String insertRows) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTable);
                statement.execute(insertRows);
            }

            QueryEngine engine = engine(Dialects.of(connection.getMetaData().getDatabaseProductName()));
            Instant     cutoff = Instant.now().minus(10, ChronoUnit.DAYS);

            assertEquals(List.of(1), identifiers(connection, engine.compileFilter("events", RECENT)));
            assertEquals(List.of(2), identifiers(connection, engine.compileFilter("events", BEFORE, Map.of("cutoff", cutoff))));
        }
    }

    private static List<Integer> identifiers(Connection connection, Fragment statement) throws SQLException {
        List<Integer> identifiers = new ArrayList<>();

        try (PreparedStatement prepared = connection.prepareStatement(statement.sql())) {
            for (int index = 0; index < statement.parameters().size(); index++) {
                prepared.setObject(index + 1, statement.parameters().get(index));
            }

            try (ResultSet rows = prepared.executeQuery()) {
                while (rows.next()) {
                    identifiers.add(rows.getInt("id"));
                }
            }
        }

        return identifiers;
    }

    private static QueryEngine engine(Dialect dialect) {
        return QueryEngine.with(dialect).sources(SOURCES).build();
    }
}
