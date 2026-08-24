package org.jmouse.query.sql.smoke.demo;

import org.jmouse.query.sql.QueryEngine;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.jmouse.query.sql.smoke.demo.Demo.configuration;
import static org.jmouse.query.sql.smoke.demo.Demo.engine;
import static org.jmouse.query.sql.smoke.demo.Demo.query;
import static org.jmouse.query.sql.smoke.demo.Demo.refuse;
import static org.jmouse.query.sql.smoke.demo.Demo.result;
import static org.jmouse.query.sql.smoke.demo.Demo.say;
import static org.jmouse.query.sql.smoke.demo.Demo.scenario;
import static org.jmouse.query.sql.smoke.demo.Demo.section;

/**
 * 1 · Клініка — <strong>a URL</strong>. The registrar looks for appointments left hanging.
 *
 * <pre>
 *   ?jmq:filter=visit[status] == "scheduled" and visit[date] &lt; now() and visit[doctor] in [...]
 *   &amp;jmq:order=visit[date] asc
 * </pre>
 *
 * <h2>⚠️ The two parameters are read by ONE parser, and neither is a special case</h2>
 *
 * <p>{@code jmq:filter} is compiled by {@link QueryEngine#compileFilter}, which is the least trusted
 * input this language takes and therefore the last place a schema check could be skippable. Adding an
 * order means the screen has a document, so it writes one — the same {@code view} block a saved view is,
 * assembled from what arrived rather than stored.</p>
 *
 * <h2>⚠️ The {@code jmq:} prefix is not decoration</h2>
 *
 * <p>{@code ?order=patient} (a plain column sort a table header writes) and {@code ?jmq:order=…} (an
 * expression) then coexist on the same endpoint and can never be mistaken for one another.</p>
 *
 * <h2>⚠️ The doctor list is INJECTED, never written by the caller</h2>
 *
 * <p>Which doctors this registrar may see is the server's answer, not a parameter — it is spliced into
 * the condition below. A caller that could name the list could name somebody else's.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ClinicDemo {

    /** What the browser actually sent, percent-encoding and all. */
    private static final String REQUEST =
            "/api/visits?page=0&size=20"
            + "&jmq%3Afilter=visit%5Bstatus%5D%20%3D%3D%20%22scheduled%22%20and%20visit%5Bdate%5D%20%3C%20now()"
            + "&jmq%3Aorder=visit%5Bdate%5D%20asc";

    /** The registrar's own doctors, from the session — never from the query string. */
    private static final String MINE = "['doctor-hrytsenko', 'doctor-lysenko']";

    private ClinicDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("1 · КЛІНІКА — вираз приїхав у URL");
        configuration("visits");

        QueryEngine engine = engine();

        section("REQUEST ── that the browser sent");
        say("   " + REQUEST);

        String filter = parameter(REQUEST, "jmq:filter");
        String order  = parameter(REQUEST, "jmq:order");

        query("?jmq:filter= — decoded, then narrowed to this registrar's doctors", filter
                                                                                   + " and visit[doctor] in " + MINE);
        result(engine.compileFilter("visits", filter + " and visit[doctor] in " + MINE));

        // ⚠️ An order means the screen has a document, so it writes one. Nothing is stored — this view
        // lives for the length of the request.
        String view = """
                view "URL" on visits {
                  where   %s and visit[doctor] in %s
                  columns visit[patient] as patient, visit[doctor] as doctor, visit[date] as at
                  order   %s
                }
                """.formatted(filter, MINE, order);

        query("?jmq:filter= + ?jmq:order= — assembled into the view a saved one would have been", view);
        result(engine.compileDocument(view));

        section("⚠️ REFUSED AT REQUEST TIME — 400, with the compiler's own sentence");
        refuse("visit[doctorr] — a typo in an attribute name",
                () -> engine.compileFilter("visits", "visit[doctorr] == 'x'"));
        refuse("DROP TABLE visits — not an expression at all",
                () -> engine.compileFilter("visits", "DROP TABLE visits"));

        refuse("count() > 3 — an aggregate where rows, not groups, are being chosen",
                () -> engine.compileFilter("visits", "count() > 3"));

        section("⚠️ TWO OF THOSE WERE FOUND BY THIS DEMONSTRATION, AND FIXED");
        say("     1 · the aggregate above used to compile here — `WHERE COUNT(*) > ?` — while the same");
        say("         expression inside a view was refused. The rule now lives on the condition, so a");
        say("         URL is checked exactly as a document is.");
        say("     2 · a syntax error used to arrive as an Error rather than an Exception, so the");
        say("         `catch (Exception)` around a request handler never saw it and a malformed filter");
        say("         left as a 500. It is an ordinary exception now, and the sentence reaches the caller.");

        boolean caught = false;

        try {
            engine.compileFilter("visits", "visit[status] ==");
        } catch (Exception refusal) {
            caught = true;
        }

        Demo.note(caught, "a malformed filter is caught by `catch (Exception)` — a 400 with a reason");
    }

    /** ⚠️ Percent-decoded once, exactly as a servlet container would hand it over. */
    private static String parameter(String request, String name) {
        String encoded = URLDecoder.decode(request, StandardCharsets.UTF_8);

        for (String pair : encoded.substring(encoded.indexOf('?') + 1).split("&")) {
            int equals = pair.indexOf('=');

            if (equals > 0 && pair.substring(0, equals).equals(name)) {
                return pair.substring(equals + 1);
            }
        }

        throw new IllegalArgumentException("the request carries no '%s'".formatted(name));
    }
}
