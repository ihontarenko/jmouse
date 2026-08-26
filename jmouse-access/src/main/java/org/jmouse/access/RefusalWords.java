package org.jmouse.access;

import org.jmouse.access.spi.GrantAttribution;

/**
 * One place that joins the sentence a policy author wrote to the sentence the engine wrote.
 *
 * <p>A rule may carry {@code reason "…"}, and the whole point of that field is that somebody hits the
 * refusal and reads it. The engine's own words explain the <em>mechanism</em> — which condition, which
 * scope, which permission — and the author's words explain the <em>decision</em>. A refusal wants both,
 * and it wants them spelled the same way wherever it is raised.
 *
 * <h2>⚠️ The author's sentence goes FIRST</h2>
 *
 * <p>It used to be a tail: {@code "This is denied where `…` with reason '…'."} That puts the only part
 * written for the reader behind the part written for whoever administers the installation, and a person
 * who pressed a button stops reading at the backtick. Leading with it costs the administrator nothing —
 * the quote is still there, still verbatim, still searchable — and it costs the reader nothing to skip.
 *
 * <h2>⚠️ Why this is not a private method on the axis that needed it first</h2>
 *
 * <p>It was one. {@link org.jmouse.access.axis.ConditionAxis} grew the tail privately, because the
 * condition axis is where a rule most obviously wants a sentence — and every other axis went on refusing
 * in the product's own words with the author's sentence dropped on the floor. A permission taken away by
 * an explained {@code deny} is the case that hurts most: the effective set carries who removed it and
 * why, and the reader is told to go and ask somebody to grant it — advice that cannot work, because deny
 * wins last.
 *
 * <p>⚠️ And the words are only half of it. An explanation glued into a sentence is an explanation no
 * client can render on its own, so {@link AccessDecision} carries it as a field beside them. This class
 * decides how the two READ; it is not where the fact lives.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RefusalWords {

    private RefusalWords() {
    }

    /**
     * The author's sentence, then the engine's.
     *
     * <p>⚠️ <strong>{@code words} is a stem, not a finished sentence.</strong> The full stop is added
     * here so that a caller which had already closed its own does not produce a double one. A trailing
     * stop is tolerated and removed rather than refused: this is wording, and wording that throws is
     * worse than wording that reads slightly differently than intended.
     *
     * @param words       what the engine has to say
     * @param explanation what the policy author wrote for whoever is refused, or null
     * @return one or two sentences, the author's first, ending in a full stop
     */
    public static String explained(String words, String explanation) {
        String written = closed(words);

        if (explanation == null || explanation.isBlank()) {
            return written;
        }

        String sentence = closed(explanation);

        return written.isEmpty() ? sentence : sentence + " " + written;
    }

    /** The same, reading the sentence off the rule that decided this. */
    public static String explained(String words, GrantAttribution attribution) {
        return explained(words, attribution == null ? null : attribution.explanation());
    }

    /** The same, reading it off the rule that took the permission away. */
    public static String explained(String words, PermissionProvenance provenance) {
        return explained(words, provenance == null ? null : provenance.explanation());
    }

    /**
     * One sentence, ending exactly once.
     *
     * <p>⚠️ {@code !} and {@code ?} close a sentence too, and replacing either with a full stop would
     * rewrite what somebody typed. The only thing normalised is a missing stop and a repeated one.
     */
    private static String closed(String sentence) {
        String written = sentence == null ? "" : sentence.strip();

        while (written.endsWith(".")) {
            written = written.substring(0, written.length() - 1).strip();
        }

        if (written.isEmpty()) {
            return "";
        }

        return written.endsWith("!") || written.endsWith("?") ? written : written + ".";
    }
}
