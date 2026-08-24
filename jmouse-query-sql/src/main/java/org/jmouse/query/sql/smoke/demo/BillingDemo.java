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
 * 4 · SaaS-білінг — <strong>a {@code .jmq} file</strong>: one definition, many views.
 *
 * <pre>
 * function active(moment) { where subscription[from] &lt;= moment
 *                                 and (subscription[until] is null or subscription[until] &gt; moment) }
 * function paying()       { where subscription[plan] != 'free' }
 *
 * view "Хто платить сьогодні" on subscriptions {
 *   where   active(now()) and paying()
 *   columns subscription[customer] as customer, subscription[plan] as plan, subscription[mrr] as mrr
 *   order   subscription[mrr] desc
 * }
 * </pre>
 *
 * <h2>⚠️ What "active" means is written ONCE</h2>
 *
 * <p>Three clauses and a null case — the definition every report in a billing system gets subtly wrong
 * in its own way. Here the churn report, the revenue tile and this list call the same body, so they
 * cannot come to disagree about who is a customer.</p>
 *
 * <h2>⚠️ A call is INLINED before anything downstream sees it</h2>
 *
 * <p>The checker, the compiler and any future backend are handed one ordinary condition and never learn
 * that functions exist. A function's parameters are the reason the order matters: a checker asked about
 * {@code moment} would refuse it as an attribute nothing declares.</p>
 *
 * <p>⚠️ And the parameter is not called {@code on} — that word opens a view's target ({@code on
 * subscriptions}) and the parser reads it as one. A reserved word is a poor parameter name in any
 * language; here it is refused rather than misread.</p>
 *
 * <h2>⚠️ A function body is one query, not a procedure</h2>
 *
 * <p>No loops, no recursion, no assignment. That is what keeps jMQ total — every call terminates,
 * so nothing here needs a sandbox or a timeout.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class BillingDemo {

    /** The library — in a real product, its own file, imported by every report. */
    private static final String LIBRARY = """
            function active(moment) {
              where subscription[from] <= moment and (subscription[until] is null or subscription[until] > moment)
            }

            function paying() {
              where subscription[plan] != 'free'
            }
            """;

    private BillingDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("4 · SaaS-БІЛІНГ — бібліотека функцій у .jmq, і в'юхи, що її кличуть");
        configuration("subscriptions");

        QueryEngine engine = engine();

        section("LIBRARY ── one definition of what \"active\" means");
        System.out.println(LIBRARY.indent(5).stripTrailing());

        String paying = LIBRARY + """
                view "Хто платить сьогодні" on subscriptions {
                  where   active(now()) and paying()
                  columns subscription[customer] as customer, subscription[plan] as plan,
                          subscription[mrr] as mrr
                  order   subscription[mrr] desc
                }
                """;

        query("active(now()) and paying()", paying);
        result(engine.compileDocument(paying));

        // ⚠️ The same body, a different moment — which is the whole reason `on` is a parameter rather
        // than a `now()` baked into the definition.
        String lastYear = LIBRARY + """
                view "Хто платив рік тому" on subscriptions {
                  where   active(now() - days(365))
                  columns subscription[customer] as customer, subscription[plan] as plan,
                          subscription[from] as since, subscription[until] as untill
                  order   subscription[customer] asc
                }
                """;

        query("active(now() - days(365)) — the same definition, asked about another day", lastYear);
        result(engine.compileDocument(lastYear));

        String revenue = LIBRARY + """
                view "MRR за планом" on subscriptions {
                  where   active(now()) and paying()
                  columns subscription[plan] as plan, count() as customers, sum(subscription[mrr]) as mrr
                  group   subscription[plan]
                  order   sum(subscription[mrr]) desc
                }
                """;

        query("and a report over the same two functions — nothing about them changes", revenue);
        result(engine.compileDocument(revenue));

        section("⚠️ REFUSALS — a call is checked like anything else");
        refuse("active() — the wrong number of arguments",
                () -> engine.compileDocument(LIBRARY + """
                        view "v" on subscriptions { where active() columns subscription[plan] as plan }
                        """));
        refuse("churned() — a function nothing declares",
                () -> engine.compileDocument(LIBRARY + """
                        view "v" on subscriptions { where churned() columns subscription[plan] as plan }
                        """));

        say("");
        say("⚠️ NOTE: `function active(on as date)` з початкового ескізу — не той синтаксис. Параметр");
        say("   пишеться `name` або `name : default`: `:` у спільному парсері вже означає значення за");
        say("   замовчуванням, і забрати його — це змінити сенс кожного jME-виразу в чотирьох продуктах.");
        say("   А `on` до того ж зайняте: воно відкриває ціль в'юхи (`on subscriptions`).");
    }
}
