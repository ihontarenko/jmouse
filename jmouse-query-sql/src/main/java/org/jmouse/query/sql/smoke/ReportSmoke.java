package org.jmouse.query.sql.smoke;

import org.jmouse.jdbc.dialect.PostgreSqlDialect;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.QueryEngine;

import static org.jmouse.query.sql.smoke.Smokes.TESSERA;
import static org.jmouse.query.sql.smoke.Smokes.banner;
import static org.jmouse.query.sql.smoke.Smokes.execute;
import static org.jmouse.query.sql.smoke.Smokes.jmq;
import static org.jmouse.query.sql.smoke.Smokes.note;
import static org.jmouse.query.sql.smoke.Smokes.refuse;
import static org.jmouse.query.sql.smoke.Smokes.summary;

/**
 * Aggregation and the clock — what turns a <em>view</em> into a <em>report</em>.
 *
 * <pre>
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.ReportSmoke
 * </pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ReportSmoke {

    public static void main(String[] arguments) {
        QueryEngine tssr = Catalog.tessera();

        banner("1 · COUNT BY STATUS — the smallest report there is");
        run(tssr, """
                view "Скільки в якому статусі" on issues {
                  columns issue.status as status, count() as many
                  group   issue.status
                  order   count() desc
                }
                """);

        banner("2 · SUM AND AVERAGE, WITH `having` — filtering the GROUPS");
        run(tssr, """
                view "Вага за типом" on issues {
                  columns issue.type as type, count() as many,
                          sum(issue.points) as total, avg(issue.points) as average
                  group   issue.type
                  having  sum(issue.points) > 40
                  order   sum(issue.points) desc
                }
                """);

        banner("3 · `where` AND `having` TOGETHER — rows first, then groups");
        run(tssr, """
                view "Закриті, за виконавцем" on issues {
                  where   issue.status == 'status-done'
                  columns issue.assignee as who, count() as many, max(issue.points) as biggest
                  group   issue.assignee
                  having  count() > 30
                  order   count() desc
                }
                """);

        banner("4 · min / max OVER A GROUP");
        run(tssr, """
                view "Розкид оцінок" on issues {
                  where   issue.points > 0
                  columns issue.type as type, min(issue.points) as least, max(issue.points) as most
                  group   issue.type
                  order   max(issue.points) desc
                }
                """);

        banner("⚠️ 5 · THE CLOCK — `now()` is bound ONCE, not left to the database");
        run(tssr, """
                view "За останній рік" on issues {
                  where   issue.opened > now() - days(365)
                  columns issue.key as k, issue.opened as opened
                  order   issue.opened desc
                }
                """);

        banner("6 · OTHER UNITS — hours, weeks, months");
        run(tssr, """
                view "За три місяці" on issues {
                  where   issue.opened > now() - months(3) and issue.points > 8
                  columns issue.key as k, issue.points as pts, issue.opened as opened
                  order   issue.opened desc
                }
                """);

        banner("7 · COUNTING OVER A WINDOW — the clock and a group together");
        run(tssr, """
                view "Створено за рік, за типом" on issues {
                  where   issue.opened > now() - years(1)
                  columns issue.type as type, count() as many
                  group   issue.type
                  order   count() desc
                }
                """);

        banner("⚠️ 8 · THE SAME REPORT ON POSTGRESQL — the interval is written differently");
        Fragment postgres = tssr.forDialect(new PostgreSqlDialect()).compileDocument("""
                view "За рік" on issues {
                  columns issue.type as type, count() as many
                  where   issue.opened > now() - days(365)
                  group   issue.type
                }
                """);

        jmq("issue.opened > now() - days(365)      — dialect: postgresql");
        System.out.println("  SQL ──");
        System.out.println("     " + postgres.sql());
        note(postgres.sql().contains("INTERVAL '1 DAY'"),
                "written as `x - ? * INTERVAL '1 DAY'`, not MySQL's DATE_SUB  ok");

        banner("⚠️ 9 · REFUSALS — each names the clause, not a database error");
        refuse(() -> tssr.compileDocument("""
                view "v" on issues { where count() > 3 columns issue.key as k }
                """));
        refuse(() -> tssr.compileDocument("""
                view "v" on issues { where issue.points > 1 and count() > 3 columns issue.key as k }
                """));
        refuse(() -> tssr.compileDocument("""
                view "v" on issues { columns sum() as total group issue.type }
                """));
        refuse(() -> tssr.compileDocument("""
                view "v" on issues { where issue.points > days(7) columns issue.key as k }
                """));
        refuse(() -> tssr.compileDocument("""
                view "v" on issues { columns median(issue.points) as m }
                """));

        summary();
    }

    private static void run(QueryEngine engine, String source) {
        jmq(source);

        try {
            execute(TESSERA, engine.compileDocument(source), 6);
        } catch (Exception exception) {
            note(false, "<<< " + exception.getMessage());
        }
    }
}
