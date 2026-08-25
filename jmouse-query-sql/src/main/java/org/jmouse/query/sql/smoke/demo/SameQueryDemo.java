package org.jmouse.query.sql.smoke.demo;

import org.jmouse.query.translate.row.RowTranslator;
import org.jmouse.query.el.node.ViewNode;
import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.sql.QuerySource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * One query text, two completely different things underneath — a database and a parsed file.
 *
 * <pre>
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.demo.SameQueryDemo
 * </pre>
 *
 * <p>The question this answers is the one a person actually asks: <em>"can I write the filter once and
 * run it against the table, and against a CSV somebody just uploaded, without touching it?"</em></p>
 *
 * <h2>⚠️ What "the same" means, precisely</h2>
 *
 * <table>
 *   <caption>Two backends, one meaning</caption>
 *   <tr><th></th><th>SQL</th><th>in memory</th></tr>
 *   <tr><th>an attribute</th><td>a column, or a joined column</td><td>a key in the row's map</td></tr>
 *   <tr><th>a supplied value</th><td>a bound {@code ?}</td><td>a variable in the evaluation context</td></tr>
 *   <tr><th>a list value</th><td>{@code ?, ?, ?} — as many as there are</td><td>a {@code List}, as it is</td></tr>
 *   <tr><th>what checks it</th><td colspan="2">the same {@code QuerySchema}, and the same refusals</td></tr>
 * </table>
 *
 * <p>⚠️ The two backends declare different {@link org.jmouse.query.translate.Capabilities}, and that is the
 * honest part: the in-memory one has no {@code JOIN} and no {@code AGGREGATE}, so a query needing either
 * is <strong>refused by name</strong> rather than answered approximately. A file has no second table to
 * join to — but it can carry the joined value as a column of its own, which is what the CSV below does.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SameQueryDemo {

    /**
     * ⚠️ Written once, used twice, and never edited between the two runs.
     *
     * <p>{@code in} rather than {@code is hasAny(…)}: the attribute holds <strong>one</strong> member, and
     * the list is on the other side. {@code hasAny} asks the opposite question — whether an attribute
     * that holds MANY values contains any of these — and is what a collection takes.</p>
     */
    private static final String FILTER =
            "ticket[level] > 1 and ticket[member] in blockedIds";

    private SameQueryDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("15 · ОДИН ТЕКСТ — БАЗА І РОЗІБРАНИЙ CSV");
        configuration("tickets");

        QueryEngine engine = engine();
        QuerySource source = engine.source("tickets").orElseThrow();

        Map<String, Object> supplied = Map.of("blockedIds", List.of("member-3", "member-4"));

        say("");
        say("   blockedIds = " + supplied.get("blockedIds") + "   ← від викликача, не з тексту запиту");
        query("один фільтр, який далі ніхто не редагує", FILTER);

        // ── 1 · the database ──────────────────────────────────────────────────────────────────────
        section("1 · ПРОТИ БАЗИ — компілюється в SQL");
        result(engine.compileFilter("tickets", FILTER, supplied));

        // ── 2 · the same text over a parsed file ──────────────────────────────────────────────────
        section("2 · ПРОТИ CSV — той самий текст, жодної зміни");

        List<Map<String, Object>> rows = csv("jmq/tickets.csv");

        say("   рядків у файлі: " + rows.size() + "; ключі — ті самі імена, що пише запит");

        ViewNode view = engine.language()
                .document("view \"csv\" on tickets { where %s }".formatted(FILTER))
                .getSingleView()
                .orElseThrow();

        RowTranslator memory = new RowTranslator(
                source.schema(), engine.language().expressionLanguage(), supplied);

        List<Map<String, Object>> matched = memory.translate(view).run(rows);

        matched.forEach(row -> say("     " + row.get("ticket[title]")
                                   + "  ·  level=" + row.get("ticket[level]")
                                   + "  ·  " + row.get("ticket[member].name")));

        note(matched.size() == 2, "CSV повернув " + matched.size() + " — стільки ж, скільки база");

        section("⚠️ 3 · CSV ТРИМАЄ ТЕКСТ, І ЦЕ ВИДНО");
        say("   `ticket[level]` у файлі — рядок \"3\", а не число. Схема каже number, тож порівняння");
        say("   пропускається — і в пам'яті це працює лише тому, що jME порівнює числа й текст м'яко.");
        say("   ⚠️ Для файлу, про який нічого не обіцяно, чесна декларація — `unknown` і `| int`:");

        String typed = "ticket[level] | int > 1 and ticket[member] in blockedIds";

        query("те саме з явним конвертером", typed);
        say("   SQL: " + engine.compileFilter("tickets", typed, supplied).sql());
        note(memory.translate(engine.language()
                        .document("view \"csv\" on tickets { where %s }".formatted(typed))
                        .getSingleView().orElseThrow())
                     .run(rows).size() == 2,
                "і в пам'яті — ті самі два рядки");

        section("⚠️ 4 · ЩО ФАЙЛ НЕ ВМІЄ — і каже про це");
        refuse("group/having над файлом — memory не оголошує AGGREGATE",
                () -> memory.translate(engine.language().document("""
                        view "v" on tickets {
                          columns ticket[member] as who, count() as many
                          group   ticket[member]
                        }
                        """).getSingleView().orElseThrow()));

        section("⚠️ 5 · СПИСОК ТАМ, ДЕ ПОТРІБНЕ ОДНЕ ЗНАЧЕННЯ — відмова, а не кривий SQL");
        refuse("ticket[member] == blockedIds",
                () -> engine.compileFilter("tickets", "ticket[member] == blockedIds", supplied));

        section("⚠️ 6 · ПОРОЖНІЙ СПИСОК — теж питання, і в нього є чесна відповідь");
        say("   SQL: " + engine.compileFilter(
                "tickets", FILTER, Map.of("blockedIds", List.of())).sql());
        say("   ⚠️ `IN ()` — синтаксична помилка в обох базах; `1 = 0` каже те саме і нічого не зв'язує.");
    }

    /**
     * ⚠️ A CSV whose header row is the names the query writes. That is the whole adapter: an in-memory
     * row is a map keyed by the attribute a query names, so a file only has to say which column is which.
     */
    private static List<Map<String, Object>> csv(String resource) {
        try (InputStream stream = SameQueryDemo.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("'%s' is not on the classpath".formatted(resource));
            }

            List<String> lines = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .lines()
                    .filter(line -> !line.isBlank())
                    .toList();

            String[]                  header    = lines.getFirst().split(";");
            List<Map<String, Object>> collected = new ArrayList<>();

            for (String line : lines.subList(1, lines.size())) {
                String[]            cells = line.split(";");
                Map<String, Object> row   = new LinkedHashMap<>();

                for (int index = 0; index < header.length && index < cells.length; index++) {
                    row.put(header[index], cells[index]);
                }

                collected.add(row);
            }

            return collected;
        } catch (Exception unreadable) {
            throw new IllegalStateException(unreadable);
        }
    }
}
