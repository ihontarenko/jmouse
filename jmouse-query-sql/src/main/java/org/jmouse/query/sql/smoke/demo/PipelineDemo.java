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
 * 8 · CI/CD — <strong>a terminal</strong>. One line, and not a file anywhere.
 *
 * <pre>
 *   buildctl query 'where build[branch] == "master" and build[status] == "failed"
 *                         and build[at] &gt; now() - hours(24)'
 * </pre>
 *
 * <h2>⚠️ The `where` keyword survives outside a document, and that is deliberate</h2>
 *
 * <p>A command line writes the same clause a file writes. There is no "CLI syntax" to learn, nothing to
 * quote differently, and a line that worked in a terminal can be pasted into a saved view unchanged —
 * which is the property the two entry points exist to keep.</p>
 *
 * <h2>⚠️ What a command-line tool adds is a COUNT, not another language</h2>
 *
 * <p>{@code --count} selects over the same compiled condition. The parts come back from the compiler
 * rather than a finished statement precisely so a caller can select something else over them.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class PipelineDemo {

    private static final String COMMAND =
            "buildctl query 'where build[branch] == \"master\" and build[status] == \"failed\" "
            + "and build[at] > now() - hours(24)'";

    private PipelineDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("8 · CI/CD — один рядок у терміналі, без жодного файлу");
        configuration("builds");

        QueryEngine engine = engine();

        section("THE COMMAND ── as somebody types it");
        say("   $ " + COMMAND);

        String argument = COMMAND.substring(COMMAND.indexOf('\'') + 1, COMMAND.lastIndexOf('\''));
        String filter   = argument.substring("where ".length());

        query("the argument, with `where` stripped — one bare expression", filter);
        result(engine.compileFilter("builds", filter));

        String report = """
                view "Впало за добу, за гілками" on builds {
                  where   build[status] == 'failed' and build[at] > now() - hours(24)
                  columns build[branch] as branch, count() as failures,
                          max(build[at]) as latest, avg(build[duration]) as seconds
                  group   build[branch]
                  order   count() desc
                }
                """;

        query("⚠️ the same line, grown into a report — nothing about the condition changed", report);
        result(engine.compileDocument(report));

        section("⚠️ WHAT A `--json` FLAG WOULD PRINT — the query as data, without a database");
        say("   " + engine.language().expression(filter).toSource());
        say("   ⚠️ `and`, не `&&` — un-parse віддає те написання, яке людина ввела. Для CLI це");
        say("      косметика; для білдера, що зберігає відредаговану в'юху, символи замість слів були б");
        say("      діфом, якого ніхто не робив.");

        section("⚠️ AND A LINE THAT IS NOT ONE");
        refuse("build[brunch] — the typo everybody makes",
                () -> engine.compileFilter("builds", "build[brunch] == 'master'"));
        refuse("`select * from builds` — another language entirely",
                () -> engine.compileFilter("builds", "select * from builds"));

        say("");
        say("⚠️ NOTE: `now() - 24h` з початкового ескізу мовчки означає `now() - 24` — лексер читає");
        say("   суфікс як тип числа і губить його. Тому тривалості пишуться функціями: hours(24).");
    }
}
