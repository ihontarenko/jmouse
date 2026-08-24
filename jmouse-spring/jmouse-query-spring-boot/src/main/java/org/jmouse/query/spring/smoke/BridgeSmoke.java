package org.jmouse.query.spring.smoke;

import org.jmouse.jdbc.dialect.MySqlDialect;
import org.jmouse.query.spring.JmQuery;
import org.jmouse.query.spring.Parameter;
import org.jmouse.query.spring.QueryRepositories;
import org.jmouse.query.sql.QueryEngine;

import javax.sql.DataSource;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * {@code @JmQuery} on a repository method, running for real.
 *
 * <pre>
 *   mvn -pl jmouse-spring/jmouse-query-spring-boot exec:java \
 *       -Dexec.mainClass=org.jmouse.query.spring.smoke.BridgeSmoke
 * </pre>
 *
 * <p>⚠️ <strong>No Spring context here on purpose.</strong> What this demonstrates is the bridge's own
 * work — compile at creation, bind by name, map a row onto a record — and a demonstration that booted an
 * application would be demonstrating Spring. The autoconfiguration is twenty lines above this and does
 * one thing: hand the same two objects to the same factory.</p>
 *
 * <p>It reads {@code jmq_demo}, the schema the ten worked examples build; run {@code DemoSmoke} first if
 * the tables are not there.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class BridgeSmoke {

    private static final String URL = "jdbc:mysql://localhost:3306/jmq_demo"
                                      + "?useSSL=false&allowPublicKeyRetrieval=true"
                                      + "&serverTimezone=UTC&characterEncoding=UTF-8";

    private static int passed;
    private static int failed;

    static {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
    }

    private BridgeSmoke() {
    }

    /** One delivery, as a record the bridge fills by component name. */
    public record Delivery(String reference, String carrier, String city, java.math.BigDecimal weightKg) {
    }

    /** What a product writes — and the whole of what it writes. */
    public interface DeliveryQueries {

        @JmQuery(source = "deliveries",
                 value = "where delivery[carrier] == carrier and delivery[weight] > minWeight",
                 select = "d.reference, d.carrier, d.city, d.weight_kg")
        List<Delivery> heavyFor(@Parameter("carrier") String carrier,
                                @Parameter("minWeight") double minWeight);

        @JmQuery(source = "deliveries", value = "delivery[reference] == reference",
                 select = "d.reference, d.carrier, d.city, d.weight_kg")
        Optional<Delivery> byReference(@Parameter("reference") String reference);

        @JmQuery(source = "deliveries", value = "delivery[carrier] == carrier", select = "COUNT(*)")
        long countFor(@Parameter("carrier") String carrier);

        @JmQuery(source = "deliveries", value = "delivery[city] == city", select = "1")
        boolean anyIn(@Parameter("city") String city);

        @JmQuery(value = """
                view "Найважче за перевізником" on deliveries {
                  columns delivery[carrier] as carrier, count() as shipments,
                          max(delivery[weight]) as heaviest
                  group   delivery[carrier]
                  order   max(delivery[weight]) desc
                }
                """)
        List<java.util.Map<String, Object>> byCarrier();
    }

    /** ⚠️ Every one of these is meant to fail the BOOT, not a request. */
    public interface BrokenQueries {

        @JmQuery(source = "deliveries", value = "delivery[carier] == carrier")
        List<Delivery> misspelled(@Parameter("carrier") String carrier);
    }

    public interface Unnamed {

        @JmQuery(source = "deliveries", value = "delivery[carrier] == carrier")
        List<Delivery> unnamed(String carrier);
    }

    public interface WithoutSource {

        @JmQuery("delivery[carrier] == carrier")
        List<Delivery> nowhere(@Parameter("carrier") String carrier);
    }

    public static void main(String[] arguments) {
        QueryEngine engine = QueryEngine.with(new MySqlDialect()).sources(read("jmq/demo.jmq")).build();
        QueryRepositories repositories = new QueryRepositories(engine, new PlainDataSource());

        banner("1 · THE REPOSITORY, CREATED — every query compiled before a single call");
        DeliveryQueries queries = repositories.create(DeliveryQueries.class);
        note(true, "DeliveryQueries created; five methods parsed and checked against their sources");

        banner("2 · A LIST OF RECORDS — bound by name, mapped by component");
        List<Delivery> heavy = queries.heavyFor("meest", 100);

        heavy.forEach(delivery -> say("   " + delivery));
        note(heavy.size() == 2, "meest over 100 kg: " + heavy.size());

        List<Delivery> light = queries.heavyFor("ukrposhta", 1);

        note(light.size() == 2, "the SAME compiled query, other values: " + light.size());

        banner("3 · ONE ROW, A COUNT, AND A YES/NO — the return type decides");
        Optional<Delivery> one = queries.byReference("DL-1003");

        note(one.isPresent() && one.get().city().equals("Одеса"), "byReference: " + one.orElse(null));
        note(queries.countFor("nova-poshta") == 3, "countFor(nova-poshta) = " + queries.countFor("nova-poshta"));
        note(queries.anyIn("Львів"), "anyIn(Львів) = true");
        note(!queries.anyIn("Ужгород"), "anyIn(Ужгород) = false");

        banner("4 · AND A WHOLE VIEW ON A METHOD — same annotation, no `source` needed");
        queries.byCarrier().forEach(row -> say("   " + row));
        note(queries.byCarrier().size() == 3, "three carriers");

        banner("⚠️ 5 · VALUES ARE BOUND — a carrier nobody has, not an incident");
        List<Delivery> injected = queries.heavyFor("'; DROP TABLE deliveries; --", 0);

        note(injected.isEmpty(), "the injection matched nothing, and the table is still there");
        note(queries.countFor("meest") == 2, "…proved by asking again: " + queries.countFor("meest"));

        banner("⚠️ 6 · A CROOKED ANNOTATION FAILS AT CREATION, NAMING THE METHOD");
        refuse("a misspelled attribute", () -> repositories.create(BrokenQueries.class));
        refuse("an argument the query cannot name", () -> repositories.create(Unnamed.class));
        refuse("a bare condition with no source", () -> repositories.create(WithoutSource.class));

        System.out.printf("%n=== %d passed, %d failed%n", passed, failed);
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("══ " + title);
    }

    private static void say(String line) {
        System.out.println("  " + line);
    }

    private static void note(boolean ok, String line) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }

        System.out.println("  " + (ok ? "ok  " : "<<< ") + line);
    }

    private static void refuse(String what, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  " + what + "  <<< НЕ ВІДМОВЛЕНО");
        } catch (Throwable refusal) {
            passed++;
            System.out.println("  ✗ " + what);
            System.out.println("    " + refusal.getMessage());
        }
    }

    private static String read(String resource) {
        try (InputStream stream = BridgeSmoke.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("'%s' is not on the classpath".formatted(resource));
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception unreadable) {
            throw new IllegalStateException(unreadable);
        }
    }

    /**
     * ⚠️ The smallest thing that is a {@link DataSource}, so this demonstration needs no pool and no
     * container. A product hands the bridge its real one; the bridge asks it for connections and knows
     * nothing else about it.
     */
    private static final class PlainDataSource implements DataSource {

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, "jmq_demo", "jmq_demo");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(URL, username, password);
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter writer) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> type) {
            return false;
        }
    }
}
