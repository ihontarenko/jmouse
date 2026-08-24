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
 * 9 · Освіта — <strong>a live block inside a page</strong>. The same expression, embedded in prose.
 *
 * <pre>
 * :::students jmq="where student[course] == 'math-101' and student[grade] | int &lt; 60
 *                  order student[grade] | int asc"
 * :::
 * </pre>
 *
 * <h2>⚠️ A page is the least trusted place an expression can come from</h2>
 *
 * <p>Anybody who can edit a page can write one — so the block carries the reader's own permissions, the
 * source is fixed by the block's own name, and the expression is checked exactly as a URL's is. What a
 * page cannot do is name a source the block did not.</p>
 *
 * <h2>⚠️ {@code | int} decides whether the block is right or merely plausible</h2>
 *
 * <p>{@code grade} is a VARCHAR — an import nobody promised anything about, which is the ordinary case.
 * As text, {@code "7" > "100"} and {@code "48" < "59"}: half the comparisons are right by luck. The
 * declaration says {@code unknown}, so the ordered comparison is refused until the pipe says how to read
 * it — and the block's author finds out while writing, not when a student asks why they are on a list.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class CourseDemo {

    /** The page's source, as an editor typed it. */
    private static final String PAGE = """
            ## Хто не тягне

            Нижче — ті, кому варто написати до кінця тижня.

            :::students jmq="where student[course] == 'math-101' and student[grade] | int < 60
                             order student[grade] | int asc"
            :::

            Оновлюється саме собою, разом зі сторінкою.
            """;

    private CourseDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("9 · ОСВІТА — live-блок у сторінці");
        configuration("students");

        QueryEngine engine = engine();

        section("PAGE ── the markdown, as it is stored");
        System.out.println(PAGE.indent(5).stripTrailing());

        String block = attribute(PAGE);
        String view  = """
                view "Хто не тягне" on students {
                  %s
                  columns student[name] as name, student[grade] as grade
                }
                """.formatted(block.strip());

        query("the block's jmq=\"…\", wrapped in the view the renderer runs", view);
        result(engine.compileDocument(view));

        section("⚠️ THE SAME BLOCK WITHOUT `| int` — refused, not answered wrongly");
        refuse("student[grade] < 60 over a column nobody promised is a number",
                () -> engine.compileDocument("""
                        view "v" on students { where student[grade] < 60 columns student[name] as name }
                        """));
        say("       як текст \"7\" > \"100\", і рівно половина порівнянь була б правильною випадково.");

        section("⚠️ AND THE ORDER, WITH AND WITHOUT THE PIPE");
        String asText = """
                view "За оцінкою, як за текстом" on students {
                  where   student[course] == 'math-101'
                  columns student[name] as name, student[grade] as grade
                  order   student[grade] | int asc
                }
                """;

        query("ordered by the number", asText);
        result(engine.compileDocument(asText));
        refuse("ordered by the raw column — the same refusal, in the order clause",
                () -> engine.compileDocument("""
                        view "v" on students { columns student[name] as name order student[grade] asc }
                        """));
    }

    /** The {@code jmq="…"} attribute, taken off the block the way a renderer takes it. */
    private static String attribute(String page) {
        int opening = page.indexOf("jmq=\"");

        if (opening < 0) {
            throw new IllegalStateException("this page carries no live block");
        }

        int start = opening + "jmq=\"".length();

        return page.substring(start, page.indexOf('"', start));
    }
}
