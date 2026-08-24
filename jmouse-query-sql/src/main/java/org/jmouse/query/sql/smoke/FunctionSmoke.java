package org.jmouse.query.sql.smoke;

import org.jmouse.query.sql.QueryEngine;

import static org.jmouse.query.sql.smoke.Smokes.TESSERA;
import static org.jmouse.query.sql.smoke.Smokes.banner;
import static org.jmouse.query.sql.smoke.Smokes.execute;
import static org.jmouse.query.sql.smoke.Smokes.jmq;
import static org.jmouse.query.sql.smoke.Smokes.note;
import static org.jmouse.query.sql.smoke.Smokes.refuse;
import static org.jmouse.query.sql.smoke.Smokes.summary;

/**
 * Calling a declared function — resolution, binding, defaults, nesting, recursion, arity, types.
 *
 * <pre>
 *   mvn -pl jmouse-query-sql exec:java -Dexec.mainClass=org.jmouse.query.sql.smoke.FunctionSmoke
 * </pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FunctionSmoke {

    public static void main(String[] arguments) {
        QueryEngine tssr = Catalog.tessera();

        banner("1 · A CALL IS REPLACED BY THE FUNCTION'S CONDITION");
        run(tssr, """
                function big(threshold : 5) {
                  where issue.points > threshold
                }

                view "Велике" on issues {
                  where   big(20)
                  columns issue.key as k, issue.points as pts
                  order   issue.points desc
                }
                """);

        banner("2 · THE DEFAULT IS USED WHEN NO ARGUMENT IS GIVEN");
        run(tssr, """
                function big(threshold : 5) { where issue.points > threshold }

                view "Понад дефолт" on issues {
                  where   big() and issue.key is starts('KW-')
                  columns issue.key as k, issue.points as pts
                  order   issue.key asc
                }
                """);

        banner("3 · A FUNCTION CALLING A FUNCTION");
        run(tssr, """
                function big(threshold : 5) { where issue.points > threshold }
                function huge()             { where big(21) }

                view "Величезне" on issues {
                  where   huge()
                  columns issue.key as k, issue.points as pts
                }
                """);

        banner("4 · A TYPED LIST PARAMETER — one placeholder per element");
        run(tssr, """
                function inStatus(states as string[]) {
                  where issue.status in states
                }

                view "У роботі" on issues {
                  where   inStatus(['status-wip', 'status-in-review'])
                  columns issue.key as k, issue.status as st
                  order   issue.key asc
                }
                """);

        banner("⚠️ 5 · THE SAME FUNCTION TWICE, DIFFERENT ARGUMENTS — the body is REBUILT, not reused");
        run(tssr, """
                function above(n : 0) { where issue.points > n }

                view "Між 8 і 21" on issues {
                  where   above(8) and !above(21)
                  columns issue.key as k, issue.points as pts
                  order   issue.points desc
                }
                """);

        banner("⚠️ 6 · REFUSALS — every one names the fix");
        refuse(() -> tssr.compileDocument("view \"v\" on issues { where low_stok(3) }"));
        refuse(() -> tssr.compileDocument("""
                function big(threshold : 5) { where issue.points > threshold }
                view "v" on issues { where big(1, 2, 3) }
                """));
        refuse(() -> tssr.compileDocument("""
                function needs(a) { where issue.points > a }
                view "v" on issues { where needs() }
                """));
        refuse(() -> tssr.compileDocument("""
                function loop() { where loop() }
                view "v" on issues { where loop() }
                """));
        refuse(() -> tssr.compileDocument("""
                function ping() { where pong() }
                function pong() { where ping() }
                view "v" on issues { where ping() }
                """));
        refuse(() -> tssr.compileDocument("""
                function inStatus(states as string[]) { where issue.status in states }
                view "v" on issues { where inStatus('status-wip') }
                """));
        refuse(() -> tssr.compileDocument("""
                function above(n as int : 0) { where issue.points > n }
                view "v" on issues { where above('many') }
                """));
        refuse(() -> tssr.compileDocument("""
                function empty() { order issue.key asc }
                view "v" on issues { where empty() }
                """));

        banner("⚠️ 7 · A BODY IS STILL CHECKED AFTER INLINING");
        refuse(() -> tssr.compileDocument("""
                function bad() { where issue.pointz > 5 }
                view "v" on issues { where bad() }
                """));

        summary();
    }

    private static void run(QueryEngine engine, String source) {
        jmq(source);

        try {
            execute(TESSERA, engine.compileDocument(source), 4);
        } catch (Exception exception) {
            note(false, "<<< " + exception.getMessage());
        }
    }
}
