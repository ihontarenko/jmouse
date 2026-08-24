package org.jmouse.query.el;

import java.util.Set;

/**
 * The functions jMQ answers to itself — aggregates, the clock, and durations.
 *
 * <p>Named in one place because two things need the list and must not keep separate copies: the inliner,
 * which refuses a call nothing answers to, and the compiler, which translates one. A name in only one of
 * them is either a call refused for existing or a call compiled by nobody.</p>
 *
 * <h2>⚠️ Durations are FUNCTIONS, and that is not a stylistic choice</h2>
 *
 * <p>The obvious spelling is JQL's — {@code now() - 7d}. It cannot work here, and the reason is worth
 * writing down because it is invisible until measured:</p>
 *
 * <table>
 *   <caption>What the shared lexer already does with those</caption>
 *   <tr><th>Written</th><th>Becomes</th></tr>
 *   <tr><td>{@code 7d}</td><td>{@code 7.0} — a <strong>Double</strong>; {@code d} is a type suffix</td></tr>
 *   <tr><td>{@code 30s}</td><td>{@code 30} — a <strong>Short</strong>, same reason</td></tr>
 *   <tr><td>{@code 24h}</td><td>⚠️ {@code 24} — the suffix is <strong>silently dropped</strong></td></tr>
 *   <tr><td>{@code 15m}</td><td>⚠️ {@code 15} — likewise</td></tr>
 * </table>
 *
 * <p>So {@code now() - 24h} would compile as {@code now() - 24} and answer something. Nothing raises.
 * Taking those suffixes back from the number literal would change what every jME expression in four
 * products means, to buy a spelling — {@code days(7)} costs three characters and cannot be misread.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class QueryFunctions {

    /** {@code count()} — the only aggregate that takes nothing, and means "how many rows". */
    public static final String COUNT = "count";

    public static final String SUM = "sum";
    public static final String AVERAGE = "avg";
    public static final String MINIMUM = "min";
    public static final String MAXIMUM = "max";

    /**
     * {@code now()} — the moment the query was compiled.
     *
     * <p>⚠️ <strong>Bound once, not evaluated per row.</strong> Two clauses each calling it must agree,
     * or a query spanning a second boundary returns rows satisfying neither. It is also why the compiled
     * SQL carries a bound timestamp rather than the database's own {@code NOW()}: otherwise the
     * application's clock and the server's disagree and nothing in the result says so.</p>
     */
    public static final String NOW = "now";

    public static final Set<String> AGGREGATES = Set.of(COUNT, SUM, AVERAGE, MINIMUM, MAXIMUM);

    /** {@code days(7)}, {@code hours(24)} … — a length of time, for adding to or subtracting from a moment. */
    public static final Set<String> DURATIONS = Set.of("seconds", "minutes", "hours", "days", "weeks", "months", "years");

    /** Everything the language answers to without a product contributing anything. */
    public static final Set<String> BUILT_IN = union(AGGREGATES, DURATIONS, Set.of(NOW));

    private QueryFunctions() {
    }

    /** Whether this call is an aggregate — which decides whether it may appear in a {@code where}. */
    public static boolean isAggregate(String name) {
        return AGGREGATES.contains(name);
    }

    public static boolean isDuration(String name) {
        return DURATIONS.contains(name);
    }

    @SafeVarargs
    private static Set<String> union(Set<String>... sets) {
        Set<String> all = new java.util.LinkedHashSet<>();

        for (Set<String> set : sets) {
            all.addAll(set);
        }

        return Set.copyOf(all);
    }
}
