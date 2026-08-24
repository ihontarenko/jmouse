package org.jmouse.query.sql.smoke.demo;

import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.QueryEngine;

import java.util.List;

import static org.jmouse.query.sql.smoke.demo.Demo.configuration;
import static org.jmouse.query.sql.smoke.demo.Demo.engine;
import static org.jmouse.query.sql.smoke.demo.Demo.note;
import static org.jmouse.query.sql.smoke.demo.Demo.query;
import static org.jmouse.query.sql.smoke.demo.Demo.refuse;
import static org.jmouse.query.sql.smoke.demo.Demo.result;
import static org.jmouse.query.sql.smoke.demo.Demo.say;
import static org.jmouse.query.sql.smoke.demo.Demo.scenario;
import static org.jmouse.query.sql.smoke.demo.Demo.section;

/**
 * 2 · Логістика — <strong>an annotation on a repository method</strong>, with the face of JPQL.
 *
 * <pre>{@code
 * @JmQuery("where delivery[carrier] == carrier and delivery[weight] > minWeight")
 * List<Delivery> heavyFor(@Parameter("carrier") String carrier,
 *                         @Parameter("minWeight") double minWeight);
 * }</pre>
 *
 * <p>⚠️ <strong>No {@code | double} on the weight</strong>, and the reason is the declaration: this
 * source says {@code delivery[weight] … number}, so the comparison is already an ordered one. The pipe
 * exists for {@code unknown} — a value nobody promised anything about — and writing it over a column
 * that really is numeric wraps a {@code DECIMAL} in a text guard that never matches.</p>
 *
 * <h2>⚠️ The annotation layer is JMF-92 and is NOT built — this is its core half</h2>
 *
 * <p>Everything the bridge would do at startup happens below, in the module that does exist: the text is
 * parsed once, checked against the source's schema once, and compiled once. What {@code JMF-92} adds is
 * the reflection — finding the annotation, matching {@code @Parameter} names to method arguments,
 * handing the result to a repository. It adds nothing to the language.</p>
 *
 * <h2>⚠️ Named parameters are the {@code function} tag today</h2>
 *
 * <p>A parameter is declared where a body can see it, which is a function. {@code active(carrier, 100)}
 * is what a method call becomes, and the values arrive <strong>bound</strong> — never spliced into the
 * text, so a carrier called {@code '; DROP TABLE deliveries; --} is a carrier nobody has, not an
 * incident.</p>
 *
 * <h2>⚠️ Compiled at STARTUP, not at the first call</h2>
 *
 * <p>A crooked annotation is then caught by the boot, with the attribute it misnamed — rather than by
 * the first client to hit that endpoint, weeks later, as a 500.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class LogisticsDemo {

    /** What a repository method would carry. */
    private static final String ANNOTATION =
            "where delivery[carrier] == carrier and delivery[weight] > minWeight";

    private LogisticsDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("2 · ЛОГІСТИКА — вираз живе в анотації на методі репозиторію");
        configuration("deliveries");

        QueryEngine engine = engine();

        section("THE METHOD ── as a product writes it");
        say("   @JmQuery(\"" + ANNOTATION + "\")");
        say("   List<Delivery> heavyFor(@Parameter(\"carrier\") String carrier,");
        say("                           @Parameter(\"minWeight\") double minWeight);");

        // ⚠️ The parameters are declared by a `function`, which is where a body may name them. The bridge
        // (JMF-92) writes this wrapper from the annotation and the method signature; nothing else about
        // the compilation differs.
        String declared = """
                function heavyFor(carrier, minWeight) { %s }

                view "heavyFor" on deliveries {
                  where   heavyFor('%s', %s)
                  columns delivery[reference] as reference, delivery[carrier] as carrier,
                          delivery[city] as city, delivery[weight] as kilograms
                  order   delivery[weight] desc
                }
                """;

        query("heavyFor('meest', 100) — the call the method makes", declared.formatted(ANNOTATION, "meest", 100));
        result(engine.compileDocument(declared.formatted(ANNOTATION, "meest", 100)));

        query("heavyFor('nova-poshta', 300) — same compiled text, other values",
                declared.formatted(ANNOTATION, "nova-poshta", 300));

        Fragment second = engine.compileDocument(declared.formatted(ANNOTATION, "nova-poshta", 300));
        Fragment first  = engine.compileDocument(declared.formatted(ANNOTATION, "meest", 100));

        result(second);

        section("⚠️ ONE STATEMENT, TWO SETS OF VALUES — the shape does not depend on the arguments");
        note(first.sql().equals(second.sql()), "the SQL is identical; only the bound parameters differ");
        note(!first.parameters().equals(second.parameters()),
                "%s versus %s".formatted(first.parameters(), second.parameters()));

        section("⚠️ AND THE VALUE IS BOUND, NEVER SPLICED");
        Fragment injected = engine.compileDocument(
                declared.formatted(ANNOTATION, "; DROP TABLE deliveries; --", 0));

        note(!injected.sql().contains("DROP"), "the statement carries no DROP: " + injected.sql());
        note(injected.parameters().contains("; DROP TABLE deliveries; --"),
                "it went into the parameters, where it is a carrier nobody has: " + injected.parameters());
        result(injected);

        section("⚠️ A CROOKED ANNOTATION FAILS AT STARTUP, NOT AT THE FIRST CALL");
        refuse("delivery[wieght] — a typo the boot catches",
                () -> engine.compileDocument(declared.formatted(
                        "where delivery[wieght] > minWeight", "meest", 1)));
        refuse("a parameter the signature never declared",
                () -> engine.compileDocument(declared.formatted(
                        "where delivery[weight] > maxWeight", "meest", 1)));

        section("WHAT JMF-92 STILL HAS TO ADD");
        List.of("finding @JmQuery on a repository method and compiling it once, at startup",
                "matching @Parameter names to method arguments, in either order",
                "the return: a row mapper, paging, and a count over the same condition")
                .forEach(remaining -> say("   · " + remaining));
    }
}
