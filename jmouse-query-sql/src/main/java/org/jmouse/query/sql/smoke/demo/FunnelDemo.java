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
 * 5 · CRM — <strong>a dashboard tile</strong>. An aggregation is the chart's data source.
 *
 * <pre>
 * view "Воронка за менеджерами" on deals {
 *   columns deal[owner] as owner, deal[stage] as stage, sum(deal[amount]) as total
 *   group   deal[owner], deal[stage]
 *   having  sum(deal[amount]) &gt; 10000
 *   order   sum(deal[amount]) desc
 * }
 * </pre>
 *
 * <h2>⚠️ The tile stores THIS, not a chart configuration</h2>
 *
 * <p>A dashboard that stores "group by owner, then by stage, sum amount, cut below ten thousand" as
 * JSON has invented a query language with no parser, no checker and no error messages. The tile stores
 * the query; the chart reads columns off the result and knows nothing about deals.</p>
 *
 * <h2>⚠️ {@code where} chooses ROWS, {@code having} chooses GROUPS</h2>
 *
 * <p>They are not two spellings of one thing, and putting an aggregate in a {@code where} is refused
 * with that sentence rather than with a database error four layers down.</p>
 *
 * <h2>⚠️ An alias is a name for the OUTPUT, and ordering repeats the expression</h2>
 *
 * <p>{@code order total desc} would be a name nothing in the schema declares; the sort key is the
 * aggregate again — {@code order sum(deal[amount]) desc}. MySQL happens to accept the alias and
 * PostgreSQL does not, which is exactly the kind of difference the compiler exists to keep out of a
 * saved view.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class FunnelDemo {

    private FunnelDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("5 · CRM — агрегація як джерело плитки дашборду");
        configuration("deals");

        QueryEngine engine = engine();

        String funnel = """
                view "Воронка за менеджерами" on deals {
                  columns deal[owner] as owner, deal[stage] as stage, sum(deal[amount]) as total
                  group   deal[owner], deal[stage]
                  having  sum(deal[amount]) > 10000
                  order   sum(deal[amount]) desc
                }
                """;

        query("the tile, exactly as it is stored", funnel);
        result(engine.compileDocument(funnel), 12);

        String spread = """
                view "Розкид угод" on deals {
                  columns deal[stage] as stage, count() as deals,
                          min(deal[amount]) as smallest, avg(deal[amount]) as average,
                          max(deal[amount]) as biggest
                  group   deal[stage]
                  order   count() desc
                }
                """;

        query("five aggregates over one group — the same tile, another chart", spread);
        result(engine.compileDocument(spread));

        String won = """
                view "Виграно, за менеджерами" on deals {
                  where   deal[stage] == 'won'
                  columns deal[owner] as owner, count() as deals, sum(deal[amount]) as total
                  group   deal[owner]
                  order   sum(deal[amount]) desc
                }
                """;

        query("⚠️ `where` first, then the grouping — rows are chosen before groups are formed", won);
        result(engine.compileDocument(won));

        section("⚠️ REFUSALS — each names the clause, never the database");
        refuse("an aggregate in `where`",
                () -> engine.compileDocument("""
                        view "v" on deals { where sum(deal[amount]) > 10 columns deal[owner] as owner }
                        """));
        refuse("median() — a function nothing answers to",
                () -> engine.compileDocument("""
                        view "v" on deals { columns median(deal[amount]) as m group deal[owner] }
                        """));

        say("");
        say("⚠️ NOTE: `order total desc` (за аліасом) з початкового ескізу не проходить — ключ сортування");
        say("   пишеться виразом ще раз. MySQL аліас у ORDER BY приймає, PostgreSQL — ні, і саме такі");
        say("   розбіжності компілятор і не пускає в збережену в'юху.");
    }
}
