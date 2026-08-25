package org.jmouse.query.sql.smoke;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.jdbc.dialect.PostgreSqlDialect;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.sql.ViewCompiler;

import static org.jmouse.query.sql.smoke.Smokes.INNOVENTA;
import static org.jmouse.query.sql.smoke.Smokes.TESSERA;
import static org.jmouse.query.sql.smoke.Smokes.banner;
import static org.jmouse.query.sql.smoke.Smokes.execute;
import static org.jmouse.query.sql.smoke.Smokes.expect;
import static org.jmouse.query.sql.smoke.Smokes.jmq;
import static org.jmouse.query.sql.smoke.Smokes.note;
import static org.jmouse.query.sql.smoke.Smokes.refuse;
import static org.jmouse.query.sql.smoke.Smokes.summary;

/**
 * What jMQ can do, against the two live development databases.
 *
 * <h2>Running it</h2>
 *
 * <pre>
 *   docker compose up -d mysql        # from the jMouseProjects root
 *   mvn -pl jmouse-query-sql -am compile
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.QuerySmoke
 * </pre>
 *
 * <p>Or from an IDE — it is an ordinary {@code main}.</p>
 *
 * <p>⚠️ It reads the {@code innoventa} and {@code tessera} databases and writes nothing. If MySQL is not
 * up, each section says so and the run continues.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QuerySmoke {

    public static void main(String[] arguments) {
        QueryEngine invt = Catalog.innoventa();
        QueryEngine tssr = Catalog.tessera();

        banner("1 · TAKING A VALUE APART — `3300|mΩ`, the SAME answer in memory and in SQL");

        ExpressionLanguage core = new ExpressionLanguage();
        EvaluationContext memory = core.newContext();

        memory.setValue("reading", "3300|mΩ");
        expect("in memory: reading | before(\"|\")", core.evaluate("reading | before('|')", memory), "3300");
        expect("in memory: reading | after(\"|\")", core.evaluate("reading | after('|')", memory), "mΩ");
        expect("in memory: no separator at all", core.evaluate("'240' | before('|')", memory), "240");

        run(INNOVENTA, invt, """
                view "Опір понад 2k" on inventory {
                  where   entry[resistance] | before("|") | int > 2000
                  columns entry[name] as name, entry[resistance] as raw,
                          entry[resistance] | before("|") as ohms,
                          entry[resistance] | after("|")  as unit
                }
                """);

        banner("2 · ARITHMETIC IN A PROJECTION — and three-valued logic where a price is missing");
        run(INNOVENTA, invt, """
                view "Вартість позиції" on inventory {
                  where   entry[quantity] | int > 0
                  columns entry[name] as name,
                          entry[price] | before("|") | int * entry[quantity] | int as total
                  order   entry[name] asc
                }
                """);

        banner("3 · A DIFFERENT SUBJECT AREA, THE SAME ENGINE");
        run(INNOVENTA, invt, """
                view "Прилади Keithley" on assets {
                  where   asset[manufacturer] is contains('keith')
                  columns asset[manufacturer] as maker, asset[model] as model, asset[serial] as serial
                }
                """);

        banner("4 · REAL COLUMNS — no joins, no casts, 519 rows behind it");
        run(TESSERA, tssr, """
                view "Велике й закрите" on issues {
                  where   issue.points > 4 and issue.status == 'status-done'
                  columns issue.key as k, issue.summary as title, issue.points as pts
                  order   issue.points desc, issue.key asc
                }
                """);

        banner("5 · MEMBERSHIP, NULL, NEGATION OVER A WHOLE CONDITION");
        run(TESSERA, tssr, """
                view "Не готові й не в роботі" on issues {
                  where   issue.key is starts('KW-1')
                          and !(issue.status == 'status-done' or issue.status == 'status-wip')
                  columns issue.key as k, issue.status as status
                  order   issue.key asc
                }
                """);

        banner("6 · `??` — a fallback, compiled to COALESCE");
        run(TESSERA, tssr, """
                view "Оцінка або нуль" on issues {
                  where   issue.key is starts('JMF-10')
                  columns issue.key as k, issue.points ?? 0 as pts
                  order   issue.key asc
                }
                """);

        banner("7 · A DECLARED FUNCTION, CALLED");
        run(TESSERA, tssr, """
                function big(threshold : 5)   { where issue.points > threshold }
                function unassigned()         { where issue.assignee is null }

                view "Велике й нічиє" on issues {
                  where   big(13) and unassigned()
                  columns issue.key as k, issue.points as pts
                  order   issue.points desc
                }
                """);

        banner("8 · AN AD-HOC FILTER — the URL form, checked exactly as a document is");
        jmq("issue.key is starts('JMF-9')");
        execute(TESSERA, tssr.compileFilter("issues", "issue.key is starts('JMF-9')"), 4);

        banner("9 · COMPOSITION — a caller's own condition onto a document it did not write");
        ViewCompiler.CompiledQuery parts = tssr.compile(tssr.language()
                .document("view \"v\" on issues { where issue.points > 8 columns issue.key as k, issue.points as pts }")
                .getViews().getFirst());

        jmq("where issue.points > 8        + caller: i.issue_key LIKE 'INVT-%'");
        execute(TESSERA, parts
                .and(Fragment.of("i.`issue_key` LIKE ?", "INVT-%"))
                .orderedBy(Fragment.of("i.`issue_key` ASC"))
                .select(), 4);

        banner("10 · THE SAME CONFIGURATION ON POSTGRESQL — one call, nothing re-registered");
        Fragment postgres = invt.forDialect(new PostgreSqlDialect())
                .compileFilter("inventory", "entry[name] is contains('resistor')");

        jmq("entry[name] is contains('resistor')      — dialect: postgresql");
        System.out.println("  SQL ──");
        System.out.println("     " + postgres.sql());
        System.out.println("     params: " + postgres.parameters());
        note(postgres.sql().contains("\""), "quoted PostgreSQL's way, not MySQL's  ok");

        banner("⚠️ 11 · REFUSED BEFORE THE DATABASE EVER SEES IT");
        refuse(() -> invt.compileFilter("inventory", "entry[quantity] > 5"));
        refuse(() -> invt.compileFilter("inventory", "entry[name] | split('\\\\|') | first == 'a'"));
        refuse(() -> invt.compileFilter("inventory", "entry[name] | upper == 'A'"));
        refuse(() -> tssr.compileFilter("issues", "issue.pointz > 5"));
        refuse(() -> tssr.compileFilter("backlog", "issue.points > 5"));
        // ⚠️ Both of these were written when the language refused them, and both are now legal — `limit`
        // became a clause, and a second `where` became an `and`. A refusal check for something the
        // language deliberately started allowing is a check that reports the feature as a fault.
        note(tssr.compileDocument("view \"v\" on issues { where issue.points > 1 limit 10 }")
                     .sql().contains("LIMIT"),
             "'limit' is a clause now, not a refusal  ok");
        note(tssr.compileDocument("view \"v\" on issues { where issue.points > 1 where issue.points < 9 }")
                     .sql().contains("AND"),
             "a second 'where' is an 'and', not a refusal  ok");
        refuse(() -> tssr.compileDocument("view \"v\" on issues { order issue.key asc where low_stok(3) }"));

        summary();
    }

    private static void run(String url, QueryEngine engine, String source) {
        jmq(source);

        try {
            execute(url, engine.compileDocument(source), 5);
        } catch (Exception exception) {
            note(false, "<<< " + exception.getMessage());
        }
    }
}
