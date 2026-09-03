package org.jmouse.script.el.budget;

/**
 * The four ways a script can be told it has done enough.
 *
 * <p>Named rather than numbered because every one of them ends up in a sentence somebody reads: a
 * refusal has to say <em>which</em> limit stopped the script, or the only thing an author can do is
 * raise all four and hope.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum ScriptLimit {

    /**
     * How much a dispatch may do at all — statements run, loop iterations taken, functions entered.
     *
     * <p>⚠️ <strong>It counts what a script <em>repeats</em>, not every node evaluated.</strong> The
     * two are different and the difference is worth knowing: a `for` and a function call are the only
     * constructions whose cost is not fixed by the text, so bounding those bounds the script. One
     * enormous expression is bounded already — by its own length, which the parser read once.</p>
     */
    STEPS("steps"),

    /**
     * How many times one {@code for} may go round.
     *
     * <p>Per loop rather than shared, so a runaway collection is refused where it happens instead of
     * quietly spending the whole dispatch's allowance and blaming whatever ran next.</p>
     */
    LOOP_ITERATIONS("loop iterations"),

    /** How deep a function may call a function. The only guard a script that calls itself has. */
    RECURSION_DEPTH("recursion depth"),

    /**
     * Wall time.
     *
     * <p>⚠️ Off unless a host asks for it. Reading a clock makes evaluation depend on something outside
     * the script, and a host that replays a simulation cannot have that. A host that turns it on is
     * choosing to bound latency instead of work, deliberately.</p>
     */
    DEADLINE("time");

    private final String spelling;

    ScriptLimit(String spelling) {
        this.spelling = spelling;
    }

    /**
     * How this limit is written in a sentence — "ran out of <em>steps</em>".
     *
     * @return the limit's name, as prose
     */
    public String spelling() {
        return spelling;
    }
}
