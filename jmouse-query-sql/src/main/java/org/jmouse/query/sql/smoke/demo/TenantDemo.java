package org.jmouse.query.sql.smoke.demo;

import org.jmouse.query.el.node.ViewNode;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.sql.QuerySource;
import org.jmouse.query.sql.SqlCompiler;
import org.jmouse.query.sql.SqlContext;
import org.jmouse.query.sql.ViewCompiler;

import static org.jmouse.query.sql.smoke.demo.Demo.configuration;
import static org.jmouse.query.sql.smoke.demo.Demo.engine;
import static org.jmouse.query.sql.smoke.demo.Demo.note;
import static org.jmouse.query.sql.smoke.demo.Demo.query;
import static org.jmouse.query.sql.smoke.demo.Demo.result;
import static org.jmouse.query.sql.smoke.demo.Demo.say;
import static org.jmouse.query.sql.smoke.demo.Demo.scenario;
import static org.jmouse.query.sql.smoke.demo.Demo.section;

/**
 * 10 · Медіа — <strong>row-level scoping in Java</strong>. The server adds a fragment to every query.
 *
 * <pre>{@code
 * Fragment tenant = compiler.compile(language.expression("asset[tenant] == 'acme'"));
 * Fragment statement = engine.compile(view).and(tenant).select();
 * }</pre>
 *
 * <h2>⚠️ The tenant is NEVER part of the text somebody wrote</h2>
 *
 * <p>A saved view that carried its own {@code asset[tenant] == …} would be a saved view somebody could
 * edit. The condition is compiled separately, by the server, from a value the request never carried, and
 * composed onto whatever the caller asked for — so a caller cannot remove it, widen it, or {@code OR}
 * their way around it.</p>
 *
 * <h2>⚠️ {@code and()} parenthesises both sides, which is the whole safety</h2>
 *
 * <p>{@code (their condition) AND (ours)}. A caller writing {@code a or b} would otherwise bind as
 * {@code a or (b and tenant)} and see everybody's rows through the first branch. That is a defect nothing
 * raises and no row says anything about.</p>
 *
 * <h2>⚠️ A second context means a second alias space</h2>
 *
 * <p>The tenant fragment is compiled in its own {@link SqlContext}, so its bag aliases are given a
 * prefix. Two contexts each allocating {@code f1} produce a statement whose joins collide — right on
 * this source, which has no bag, and wrong the day a bag is added.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class TenantDemo {

    /** What the signed-in viewer asked for, and all they are able to ask for. */
    private static final String ASKED = """
            view "Ефіри за тиждень" on assets {
              where   asset[kind] == 'video' and asset[published] > now() - days(7)
              columns asset[tenant] as tenant, asset[title] as title, asset[published] as published
              order   asset[published] desc
            }
            """;

    private TenantDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("10 · МЕДІА — скоуп орендаря дописує сервер, у коді");
        configuration("assets");

        QueryEngine  engine = engine();
        QuerySource  source = engine.source("assets").orElseThrow();

        query("what the viewer asked for", ASKED);

        ViewNode view = engine.language().document(ASKED).getSingleView().orElseThrow();
        ViewCompiler.CompiledQuery compiled = engine.compile(view);

        section("WITHOUT THE SCOPE ── every tenant's rows, which is the bug this prevents");
        result(compiled.select());

        // ⚠️ Its own context, with an alias prefix, so two compilations cannot allocate the same join
        // alias. `assets` has no bag today; the day it grows one, this line is why nothing breaks.
        SqlContext  context  = new SqlContext(engine.dialect(), source.schema(), source.target(), "t");
        SqlCompiler compiler = new SqlCompiler(source.mapping(), context);
        Fragment    tenant   = compiler.compile(engine.language().expression("asset[tenant] == 'acme'"));

        section("THE SCOPE ── compiled by the server, from the session and nothing else");
        say("   " + tenant.sql() + "   params: " + tenant.parameters());

        section("WITH IT ── the same view, narrowed");
        result(compiled.and(tenant).select());

        section("⚠️ A CALLER WHO TRIES TO OR THEIR WAY OUT");
        String widened = """
                view "Все підряд" on assets {
                  where   asset[kind] == 'video' or asset[kind] != 'video'
                  columns asset[tenant] as tenant, asset[title] as title
                  order   asset[title] asc
                }
                """;

        query("a condition that is true for every row", widened);

        ViewNode                   everything = engine.language().document(widened).getSingleView().orElseThrow();
        ViewCompiler.CompiledQuery scoped     = engine.compile(everything).and(tenant);

        result(scoped.select());
        note(scoped.select().sql().contains("AND"), "the tenant survived: " + scoped.select().sql());

        section("⚠️ AND THE COUNT COMES OFF THE SAME PARTS");
        result(compiled.and(tenant).select(Fragment.of("COUNT(*) AS matching")));
        say("   ⚠️ той самий скомпільований WHERE, інша проєкція — саме тому compile() віддає ЧАСТИНИ,");
        say("      а не готовий стейтмент. Пагінація і лічильник інакше рахували б різні речі.");
        say("   ⚠️ ORDER BY у лічильнику лишається — нешкідливо, але зайва робота для бази; той, хто");
        say("      рахує, має віддавати частини без сортування.");
    }
}
