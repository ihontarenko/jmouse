package org.jmouse.script.el.budget;

/**
 * A script did more than it was allowed to, and was stopped.
 *
 * <h2>⚠️ This is a failure, never a truncation</h2>
 *
 * <p>A handler stopped halfway has already called some of its facades and not the rest — the host's
 * world is half-written. Returning quietly at that point would be the exact silent failure this dialect
 * refuses everywhere else: a script that loads, runs, does half of what it says, and reports nothing. So
 * it throws, and what to do about a half-applied change is the host's to decide with the transaction it
 * owns.</p>
 *
 * <h2>⚠️ And it says which limit, and whose</h2>
 *
 * <p><em>"Out of steps at 50 000"</em> is unactionable when the author asked for 200 000 and a plan
 * clamped it: they raise their own number, nothing changes, and they conclude the feature is broken. So
 * a refusal whose limit came from a ceiling says so, in as many words.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptBudgetExceededException extends RuntimeException {

    private final transient ScriptLimit limit;
    private final           String      document;
    private final           boolean     ceiling;

    /**
     * Constructs the refusal.
     *
     * @param document what the script is called
     * @param limit    which limit stopped it
     * @param allowed  the number it ran out of
     * @param where    what it was doing — a loop, a call, a body
     * @param ceiling  whether that number came from a host's ceiling rather than from the script
     */
    public ScriptBudgetExceededException(
            String document, ScriptLimit limit, long allowed, String where, boolean ceiling) {

        super(message(document, limit, allowed, where, ceiling));
        this.limit = limit;
        this.document = document;
        this.ceiling = ceiling;
    }

    private static String message(
            String document, ScriptLimit limit, long allowed, String where, boolean ceiling) {

        String named = document == null ? "a script" : document;

        // ⚠️ Time reads differently from a count. "ran out of time at 1" is a sentence nobody can
        // parse; the number is a duration and has to be written as one.
        String sentence = limit == ScriptLimit.DEADLINE
                ? "'%s' ran out of time after %dms, in %s".formatted(named, allowed, where)
                : "'%s' ran out of %s at %d, in %s".formatted(named, limit.spelling(), allowed, where);

        return ceiling
                ? sentence + " — that is this host's ceiling rather than what the script asked for, "
                  + "so raising the number on the document will not change it"
                : sentence + " — the script asked for this limit itself";
    }

    /**
     * Returns which limit stopped the script.
     *
     * @return the limit
     */
    public ScriptLimit limit() {
        return limit;
    }

    /**
     * Returns what the script is called.
     *
     * @return the document name, or {@code null} where nothing named it
     */
    public String document() {
        return document;
    }

    /**
     * Whether the number that stopped it was a host's ceiling rather than the script's own request.
     *
     * <p>⚠️ The one thing a screen showing this failure has to branch on: an author can act on the
     * first case and only an administrator can act on the second.</p>
     *
     * @return {@code true} when a ceiling decided it
     */
    public boolean hitCeiling() {
        return ceiling;
    }
}
