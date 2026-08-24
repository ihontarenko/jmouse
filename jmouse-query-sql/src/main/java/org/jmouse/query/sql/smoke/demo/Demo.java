package org.jmouse.query.sql.smoke.demo;

import org.jmouse.jdbc.dialect.MySqlDialect;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.sql.QuerySource;
import org.jmouse.query.sql.QueryTarget;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ten worked examples, and what they all share: a database of their own, and one way of showing a query.
 *
 * <h2>What every demonstration prints, in order</h2>
 *
 * <table>
 *   <caption>The chain, end to end, on one screen</caption>
 *   <tr><th>CONFIG</th><td>the {@code source { }} block, quoted out of {@code jmq/demo.jmq} itself</td></tr>
 *   <tr><th>MAPPING</th><td>what a query writes, against what the store calls it — off the built engine</td></tr>
 *   <tr><th>jMQ</th><td>the query, exactly as its entry point delivers it</td></tr>
 *   <tr><th>SQL</th><td>the statement it compiled to, and the values bound to it</td></tr>
 *   <tr><th>ROWS</th><td>what came back</td></tr>
 * </table>
 *
 * <p>⚠️ <strong>The configuration is quoted, never re-typed.</strong> A demonstration that describes its
 * own setup in prose is a demonstration that can be wrong about it; this one slices the block out of the
 * file the engine was built from, so the two cannot disagree.</p>
 *
 * <h2>⚠️ Its own database, and why</h2>
 *
 * <p>{@code jmq_demo} — created once by hand, then dropped and rebuilt by {@link #install()} on every
 * run. The existing smokes read {@code innoventa} and {@code tessera}, which is right for them: they
 * demonstrate the language against the shape those products really have. These ten are about the
 * <em>entry points</em>, and each wants a different subject area — a clinic, a carrier, a sensor, a
 * course. None of that belongs in a product's schema, and a demonstration that seeded tables into one
 * would be a demonstration nobody could run twice.</p>
 *
 * <pre>
 *   docker exec -i shared-mysql mysql -uroot -proot -e "
 *     CREATE DATABASE IF NOT EXISTS jmq_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
 *     CREATE USER IF NOT EXISTS 'jmq_demo'@'%' IDENTIFIED BY 'jmq_demo';
 *     GRANT ALL PRIVILEGES ON jmq_demo.* TO 'jmq_demo'@'%';"
 *
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.demo.DemoSmoke
 * </pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Demo {

    public static final String URL = "jdbc:mysql://localhost:3306/jmq_demo"
                                     + "?useSSL=false&allowPublicKeyRetrieval=true"
                                     + "&serverTimezone=UTC&characterEncoding=UTF-8";

    private static final String USER = "jmq_demo";

    private static final String SOURCES = "jmq/demo.jmq";
    private static final String SCHEMA  = "jmq/demo.sql";

    private static QueryEngine engine;
    private static String      declarations;
    private static boolean     installed;

    private static int passed;
    private static int failed;

    static {
        // ⚠️ A Windows console hands Java a single-byte codepage, and every Ukrainian line below would
        // print as question marks — a demonstration nobody can read. Run the terminal at `chcp 65001`.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        quieten("org.jmouse.core.convert");
        quieten("org.jmouse.el");
    }

    private Demo() {
    }

    /**
     * The engine every demonstration runs on — ten sources, declared in a file, built once.
     *
     * <p>⚠️ Building it also <em>checks</em> it: a block that says too little, or names a type nothing
     * answers to, is refused here rather than at the first query.</p>
     *
     * @return the configured engine
     */
    public static QueryEngine engine() {
        if (engine == null) {
            engine = QueryEngine.with(new MySqlDialect()).sources(declarations()).build();
        }

        return engine;
    }

    /** Drops and rebuilds the demonstration schema. Runs once per process, whichever demo asks first. */
    public static void install() {
        if (installed) {
            return;
        }

        // ⚠️ Comments are stripped BEFORE the split, not skipped after it: that file's own header quotes
        // a shell command containing a semicolon, and a splitter that respects comments only afterwards
        // hands the driver half a sentence about Docker.
        String script = read(SCHEMA).lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .collect(Collectors.joining("\n"));

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            for (String command : script.split(";")) {
                if (!command.isBlank()) {
                    statement.execute(command);
                }
            }

            installed = true;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "the demonstration schema could not be installed into jmq_demo — is the database "
                    + "created, and MySQL up? (" + exception.getMessage() + ")", exception);
        }
    }

    public static void scenario(String title) {
        install();

        System.out.println();
        System.out.println("═".repeat(110));
        System.out.println("══ " + title);
    }

    public static void section(String title) {
        System.out.println();
        System.out.println("  " + title);
    }

    public static void say(String line) {
        System.out.println("  " + line);
    }

    /**
     * The two halves of a source's configuration: what the file declares, and what the engine made of it.
     *
     * @param name the source, as a query writes it after {@code on}
     */
    public static void configuration(String name) {
        section("CONFIG ── jmq/demo.jmq");
        System.out.println(declaration(name).indent(5).stripTrailing());

        QuerySource source = engine().source(name).orElseThrow(
                () -> new IllegalStateException("nothing is declared under '%s'".formatted(name)));
        QueryTarget target = source.target();

        section("MAPPING ── what the engine built from it");
        System.out.printf("     rows from %s AS %s, keyed by %s%n", target.table(), target.alias(), target.key());

        for (QueryAttribute attribute : source.schema().attributes()) {
            System.out.printf("     %-22s -> %-18s %-9s %s%n",
                    attribute.name(), attribute.source(),
                    attribute.type().name().toLowerCase(), attribute.access().name().toLowerCase());
        }
    }

    /** The query, as its entry point delivers it — a URL, a config value, a file, a terminal line. */
    public static void query(String label, String source) {
        section("jMQ ── " + label);
        source.strip().lines().forEach(line -> System.out.println("     " + line));
    }

    /** Prints the statement, runs it, prints what came back. */
    public static void result(Fragment statement, int show) {
        section("SQL ──");
        System.out.println("     " + statement.sql());
        System.out.println("     params: " + statement.parameters());

        try (Connection connection = connect();
             PreparedStatement prepared = connection.prepareStatement(statement.sql())) {

            List<Object> values = statement.parameters();

            for (int index = 0; index < values.size(); index++) {
                prepared.setObject(index + 1, values.get(index));
            }

            try (ResultSet rows = prepared.executeQuery()) {
                List<Map<String, Object>> collected = collect(rows);

                section("ROWS ── " + collected.size());
                collected.stream().limit(show).forEach(row -> System.out.println("     " + render(row)));

                if (collected.size() > show) {
                    System.out.println("     … ще " + (collected.size() - show));
                }

                passed++;
            }
        } catch (Exception exception) {
            failed++;
            System.out.println("  <<< " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    public static void result(Fragment statement) {
        result(statement, 6);
    }

    /**
     * Runs something that must be refused, and prints the refusal a person would have read.
     *
     * <p>⚠️ <strong>{@code Throwable}, and that is not defensiveness.</strong> A syntax error in the
     * shared expression language is thrown as an {@link Error}, not an exception — so a product that
     * wraps a URL filter in {@code catch (Exception)}, which is every sane product, does not catch a
     * malformed expression at all. It escapes as a 500 where a 400 was meant, and the caller is told
     * nothing they could act on. Written here so the demonstrations keep running; the language is where
     * it needs fixing.</p>
     */
    public static void refuse(String what, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("     " + what + "  <<< НЕ ВІДМОВЛЕНО");
        } catch (Throwable refusal) {
            passed++;
            System.out.println("     ✗ " + what);
            System.out.println("       " + refusal.getMessage());
        }
    }

    public static void note(boolean ok, String line) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }

        System.out.println("     " + (ok ? "ok  " : "<<< ") + line);
    }

    public static void summary() {
        System.out.printf("%n=== %d passed, %d failed%n", passed, failed);
    }

    /**
     * One {@code source { }} block, sliced out of the file by counting braces.
     *
     * <p>⚠️ Counting rather than matching a regular expression: a block will contain braces of its own
     * the day somebody adds one, and a pattern stopping at the first closing brace would quietly show
     * half a declaration.</p>
     *
     * @param name the source's name
     * @return the block, verbatim
     */
    public static String declaration(String name) {
        String text  = declarations();
        int    start = text.indexOf("source " + name + " {");

        if (start < 0) {
            throw new IllegalStateException("jmq/demo.jmq declares no source called '%s'".formatted(name));
        }

        int depth = 0;

        for (int index = start; index < text.length(); index++) {
            char character = text.charAt(index);

            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;

                if (depth == 0) {
                    return text.substring(start, index + 1);
                }
            }
        }

        throw new IllegalStateException("the '%s' block is never closed".formatted(name));
    }

    private static String declarations() {
        if (declarations == null) {
            declarations = read(SOURCES);
        }

        return declarations;
    }

    private static Connection connect() throws Exception {
        return DriverManager.getConnection(URL, USER, USER);
    }

    private static List<Map<String, Object>> collect(ResultSet rows) throws Exception {
        ResultSetMetaData         metadata  = rows.getMetaData();
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

    private static String read(String resource) {
        try (InputStream stream = Demo.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("'%s' is not on the classpath".formatted(resource));
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * ⚠️ Reflection, and for the same reason {@code Smokes} does it: this module has no compile
     * dependency on a logging implementation and must not gain one so a demonstration can be tidy.
     */
    private static void quieten(String logger) {
        try {
            Object   instance = org.slf4j.LoggerFactory.getLogger(logger);
            Class<?> level    = Class.forName("ch.qos.logback.classic.Level");

            instance.getClass()
                    .getMethod("setLevel", level)
                    .invoke(instance, level.getField("WARN").get(null));
        } catch (Exception quietly) {
            // Not logback, or not on the classpath — nothing to turn down.
        }
    }
}
