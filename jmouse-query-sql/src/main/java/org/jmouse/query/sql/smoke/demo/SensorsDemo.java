package org.jmouse.query.sql.smoke.demo;

import org.jmouse.query.sql.QueryEngine;

import static org.jmouse.query.sql.smoke.demo.Demo.configuration;
import static org.jmouse.query.sql.smoke.demo.Demo.engine;
import static org.jmouse.query.sql.smoke.demo.Demo.query;
import static org.jmouse.query.sql.smoke.demo.Demo.refuse;
import static org.jmouse.query.sql.smoke.demo.Demo.result;
import static org.jmouse.query.sql.smoke.demo.Demo.say;
import static org.jmouse.query.sql.smoke.demo.Demo.scenario;
import static org.jmouse.query.sql.smoke.demo.Demo.section;

/**
 * 3 · IoT — <strong>an alert rule in YAML</strong>. The expression is a configuration value.
 *
 * <pre>
 * alerts:
 *   - name: "Сенсор мовчить"
 *     when: |
 *       where  reading[at] &lt; now() - minutes(15)
 *       group  reading[sensor]
 *       having count() == 0
 *     notify: ops@…
 * </pre>
 *
 * <h2>⚠️ {@code 15m} is not a duration — it is fifteen</h2>
 *
 * <p>The shared lexer reads a suffix on a number as a <em>type</em>: {@code 7d} is the Double 7.0,
 * {@code 24h} and {@code 15m} silently drop the letter. So {@code now() - 15m} would compile, run, and
 * answer about fifteen of something. Durations are therefore functions — {@code minutes(15)} — which
 * cannot be misread.</p>
 *
 * <h2>⚠️ And the rule above is WRONG in a way SQL will not tell anybody</h2>
 *
 * <p>{@code where at < now() - minutes(15)} throws away every recent reading first; the groups that
 * survive are of old readings, and {@code count() == 0} therefore matches <strong>nothing</strong>, for
 * ever. A sensor that has gone quiet produces no row to count — absence cannot be counted, only looked
 * for. The rule that works asks the opposite question: group everything, then keep the sensors whose
 * <em>latest</em> reading is old.</p>
 *
 * <p>Both are run below, against a fixture where {@code freezer-02} last reported forty minutes ago and
 * {@code attic-01} three hours ago. The first prints zero rows and the second prints the two sensors —
 * which is the entire argument for demonstrations that show rows rather than assertions that go green.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SensorsDemo {

    private static final String YAML = """
            alerts:
              - name: "Сенсор мовчить"
                when: |
                  where  reading[at] < now() - minutes(15)
                  group  reading[sensor]
                  having count() == 0
                notify: ops@example.com
            """;

    private SensorsDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("3 · IoT — вираз як значення YAML-конфіга");
        configuration("readings");

        QueryEngine engine = engine();

        section("CONFIG ── application.yml, as an operator writes it");
        System.out.println(YAML.indent(5).stripTrailing());

        // ⚠️ The product wraps the clauses in a view before compiling. The `when:` block is not a
        // different language — it is the body of a view whose name and target the configuration already
        // knows, so writing them out again in every rule would be noise.
        String naive = wrap("Сенсор мовчить",
                "columns reading[sensor] as sensor, count() as readings\n" + read(YAML));

        query("the rule as written — where + group + having", naive);
        result(engine.compileDocument(naive));
        say("   ⚠️ порожньо, і буде порожньо завжди: `where` викинув свіжі читання ще до групування,");
        say("      а відсутність не рахується — її можна тільки шукати.");

        String correct = wrap("Сенсор мовчить", """
                columns reading[sensor] as sensor, max(reading[at]) as latest, count() as readings
                group   reading[sensor]
                having  max(reading[at]) < now() - minutes(15)
                order   max(reading[at]) asc
                """);

        query("the rule that answers the question — group everything, then keep the stale groups", correct);
        result(engine.compileDocument(correct));

        section("⚠️ THE SUFFIX TRAP, REFUSED RATHER THAN ANSWERED");
        refuse("now() - minutes(15) written as a bare duration",
                () -> engine.compileDocument(wrap("v", "where reading[at] < minutes(15)")));
        refuse("an aggregate in `where`, where rows are chosen",
                () -> engine.compileDocument(wrap("v", "where count() > 2 group reading[sensor]")));
        say("   ⚠️ але `now() - 15m` НЕ відмовляється: лексер читає 15m як 15, і правило порахує");
        say("      п'ятнадцять чогось. Саме тому тривалості — функції.");
    }

    /** The `when:` block, unindented out of the YAML the way a configuration binder hands it over. */
    private static String read(String yaml) {
        return yaml.lines()
                .filter(line -> line.stripLeading().startsWith("where")
                                || line.stripLeading().startsWith("group")
                                || line.stripLeading().startsWith("having"))
                .map(String::strip)
                .reduce("", (block, line) -> block + line + "\n");
    }

    private static String wrap(String name, String body) {
        return """
                view "%s" on readings {
                  %s
                }
                """.formatted(name, body.strip().replace("\n", "\n  "));
    }
}
