package org.jmouse.query.sql.smoke.demo;

import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.sql.SqlCompileException;

import static org.jmouse.query.sql.smoke.demo.Demo.configuration;
import static org.jmouse.query.sql.smoke.demo.Demo.engine;
import static org.jmouse.query.sql.smoke.demo.Demo.query;
import static org.jmouse.query.sql.smoke.demo.Demo.result;
import static org.jmouse.query.sql.smoke.demo.Demo.say;
import static org.jmouse.query.sql.smoke.demo.Demo.scenario;
import static org.jmouse.query.sql.smoke.demo.Demo.section;

/**
 * 6 · ATS — <strong>an agent, over the Model Context Protocol</strong>.
 *
 * <p>"Знайди сеньйорів на Go, що подались за тиждень і ще не відсіяні." The agent writes the expression
 * itself, gets it wrong, reads the refusal and corrects itself — which is the loop below, run for real
 * rather than described.</p>
 *
 * <h2>⚠️ The refusal is the product's user interface here</h2>
 *
 * <p>A tool that answers {@code 400 Bad Request} teaches nothing and the next call is the same call. A
 * refusal that names the attribute it does not have, and lists the ones it does, is a correction an
 * agent can act on without a person in the loop. That is why every message in this language names the
 * thing rather than the failure.</p>
 *
 * <h2>⚠️ The SCOPE is injected, and an expression cannot widen it</h2>
 *
 * <p>{@code compileFilter("candidates", …)} says which source the text is about. Nothing an agent writes
 * can reach another table, another tenant or another column: the schema is the confinement, and an
 * attribute nobody declared does not exist to be named.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class HiringDemo {

    /** The agent's first attempt, written from the request and nothing else. */
    private static final String FIRST = """
            candidate[position] is contains("go")
              and candidate[level] in ["senior", "staff"]
              and candidate[applied] > now() - 7d
              and candidate[status] is not in ["rejected"]
            """;

    private HiringDemo() {
    }

    public static void main(String[] arguments) {
        run();
        Demo.summary();
    }

    public static void run() {
        scenario("6 · ATS — агент пише вираз сам, і сам виправляється з тексту відмови");
        configuration("candidates");

        QueryEngine engine = engine();

        section("THE ASK ── in words");
        say("   «Знайди сеньйорів на Go, що подались за тиждень і ще не відсіяні»");

        String corrected = FIRST;

        for (int attempt = 1; attempt <= 4; attempt++) {
            query("attempt " + attempt, corrected);

            try {
                result(engine.compileFilter("candidates", corrected));
                break;
            } catch (Throwable refusal) {
                // ⚠️ Throwable, because a syntax error in the shared language is an Error rather than an
                // exception — the first attempt below trips exactly that, and `catch (Exception)` would
                // let it out. Which is also what would happen to the tool call: a 500, not a refusal an
                // agent could read and correct itself from.
                say("   ✗ " + refusal.getMessage());
                corrected = correct(corrected, refusal);
                say("   → " + whatChanged(attempt));
            }
        }

        section("⚠️ AND WHAT AN AGENT STILL CANNOT DO");
        Demo.refuse("name another table — the scope was injected, not asked for",
                () -> engine.compileFilter("candidates", "deal[amount] > 0"));
        Demo.refuse("read a column nobody declared, even one that exists",
                () -> engine.compileFilter("candidates", "candidate[salary] > 0"));

        say("");
        say("⚠️ NOTE: заперечення пишеться `!(… in […])`, і НЕ через слово `not` — у спільному лексері");
        say("   `not` є синонімом до `!=`, а не логічним запереченням. Тобто `not (x in [...])`");
        say("   читається як `!= (…)` і падає синтаксисом; логічне «ні» — це символ `!`.");
        say("⚠️ І сама відмова тут — Error, а не Exception: `catch (Exception)` навколо tool-виклику");
        say("   її не спіймає, і агент отримає 500 замість речення, з якого міг би виправитись.");
    }

    /**
     * The correction an agent makes from the refusal it just read.
     *
     * <p>⚠️ Written as a table rather than a parser: the point being demonstrated is that the message
     * carries enough to act on, not that a demonstration can parse one.</p>
     */
    private static String correct(String query, Throwable refusal) {
        if (refusal instanceof SqlCompileException || refusal.getMessage() != null) {
            return query
                    // "there is nothing called 'candidate[position]' here; this target has candidate[role] …"
                    .replace("candidate[position]", "candidate[role]")
                    // `7d` is not seven days — the lexer reads the suffix as a type and drops it.
                    .replace("now() - 7d", "now() - days(7)")
                    // ⚠️ Negation is `!`, never the word `not` — which the shared lexer reads as `!=`.
                    .replace("candidate[status] is not in [\"rejected\"]",
                            "!(candidate[status] in [\"rejected\"])");
        }

        return query;
    }

    private static String whatChanged(int attempt) {
        return switch (attempt) {
            case 1 -> "candidate[position] → candidate[role], `7d` → days(7), `is not in` → !(… in …)";
            default -> "re-read the refusal and tried again";
        };
    }
}
