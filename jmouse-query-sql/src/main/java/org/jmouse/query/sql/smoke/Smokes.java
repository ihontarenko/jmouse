package org.jmouse.query.sql.smoke;

import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.sql.Fragment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the runnable smokes share: connections, printing, and a schema built from a list.
 *
 * <p>⚠️ These classes are <strong>demonstrations, not tests</strong>. They talk to the live development
 * databases and print what they find, because the whole point is to show a query, the SQL it became, the
 * values it bound and the rows that came back — which an assertion hides. Several real defects in this
 * cluster were found by <em>reading</em> that output, not by a check going red.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Smokes {

    /** The shared MySQL from the workspace's docker-compose. */
    public static final String INNOVENTA = url("innoventa");
    public static final String TESSERA   = url("tessera");

    private static int passed;
    private static int failed;

    static {
        // ⚠️ Quietened here rather than with a logback.xml in the module's resources: a configuration file
        // shipped inside a library silently overrides the logging of every product that depends on it.
        // A smoke may adjust its own output; it must not adjust anybody else's.
        quieten("org.jmouse.core.convert");
        quieten("org.jmouse.el");
    }

    private Smokes() {
    }

    /**
     * ⚠️ Reflection because this module has <strong>no compile dependency on a logging
     * implementation</strong>, and should not gain one so that a demonstration can be tidy. A library
     * that depends on logback decides logback for every product that depends on it.
     *
     * <p>Silently does nothing where the implementation is something else, which is the correct outcome:
     * the smoke still runs, it is merely noisier.</p>
     */
    private static void quieten(String logger) {
        try {
            Object instance = org.slf4j.LoggerFactory.getLogger(logger);
            Class<?> level = Class.forName("ch.qos.logback.classic.Level");

            instance.getClass()
                    .getMethod("setLevel", level)
                    .invoke(instance, level.getField("WARN").get(null));
        } catch (Exception quietly) {
            // Not logback, or not on the classpath — nothing to turn down.
        }
    }

    private static String url(String database) {
        return "jdbc:mysql://localhost:3306/%s?useSSL=false&allowPublicKeyRetrieval=true"
               .formatted(database) + "&serverTimezone=UTC&characterEncoding=UTF-8";
    }

    /**
     * ⚠️ The database user is the database name in this workspace — `innoventa`/`innoventa`,
     * `tessera`/`tessera`. Written once here so a smoke never carries a credential of its own.
     */
    private static String user(String url) {
        return url.contains("/tessera") ? "tessera" : "innoventa";
    }

    public static void banner(String title) {
        System.out.println();
        System.out.println("══ " + title);
    }

    public static void jmq(String source) {
        System.out.println();
        System.out.println("  jMQ ──");
        source.strip().lines().forEach(line -> System.out.println("     " + line));
    }

    public static void note(boolean ok, String line) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }

        System.out.println("  " + line);
    }

    /** Prints the statement, runs it, prints what came back. */
    public static void execute(String url, Fragment statement, int show) {
        System.out.println("  SQL ──");
        System.out.println("     " + statement.sql());
        System.out.println("     params: " + statement.parameters());

        try (Connection connection = DriverManager.getConnection(url, user(url), user(url));
             PreparedStatement prepared = connection.prepareStatement(statement.sql())) {

            List<Object> values = statement.parameters();

            for (int index = 0; index < values.size(); index++) {
                prepared.setObject(index + 1, values.get(index));
            }

            try (ResultSet rows = prepared.executeQuery()) {
                List<Map<String, Object>> collected = rows(rows);

                System.out.println("  ROWS ── " + collected.size());
                collected.stream().limit(show).forEach(row -> System.out.println("     " + render(row)));

                if (collected.size() > show) {
                    System.out.println("     … " + (collected.size() - show) + " more");
                }

                passed++;
            }
        } catch (Exception exception) {
            failed++;
            System.out.println("  <<< " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    /** Runs something that must be refused, and prints the refusal. */
    public static void refuse(Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  <<< NOT REFUSED");
        } catch (Exception exception) {
            passed++;
            System.out.println("  ✗ " + exception.getMessage());
        }
    }

    public static void expect(String what, Object actual, Object wanted) {
        note(String.valueOf(wanted).equals(String.valueOf(actual)),
                "%-42s -> %-10s %s".formatted(what, actual,
                        String.valueOf(wanted).equals(String.valueOf(actual)) ? "ok" : "<<< expected " + wanted));
    }

    public static void summary() {
        System.out.printf("%n=== %d passed, %d failed%n", passed, failed);
    }

    /** ⚠️ A List of Maps — the nested shape, values exactly as the driver handed them over. */
    private static List<Map<String, Object>> rows(ResultSet rows) throws Exception {
        ResultSetMetaData metadata = rows.getMetaData();
        List<Map<String, Object>> collected = new ArrayList<>();

        while (rows.next()) {
            Map<String, Object> row = new LinkedHashMap<>();

            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                row.put(metadata.getColumnLabel(column), rows.getObject(column));
            }

            collected.add(row);
        }

        return collected;
    }

    private static String render(Map<String, Object> row) {
        List<String> cells = new ArrayList<>();

        row.forEach((key, value) -> {
            String text = String.valueOf(value);

            cells.add(key + "=" + (text.length() > 44 ? text.substring(0, 41) + "…" : text));
        });

        return "{ " + String.join(", ", cells) + " }";
    }

    /** A schema from a list of attributes, keyed by the name a query writes. */
    public static QuerySchema schema(QueryAttribute... attributes) {
        Map<String, QueryAttribute> known = new LinkedHashMap<>();

        for (QueryAttribute attribute : attributes) {
            known.put(attribute.name(), attribute);
        }

        return new QuerySchema() {

            @Override
            public Optional<QueryAttribute> attribute(String name) {
                return Optional.ofNullable(known.get(name));
            }

            @Override
            public Collection<QueryAttribute> attributes() {
                return known.values();
            }
        };
    }
}
