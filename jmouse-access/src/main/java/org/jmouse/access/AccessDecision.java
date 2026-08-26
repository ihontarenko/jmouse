package org.jmouse.access;

/**
 * The answer to "may I", and — where the answer is no — which of the five axes said so.
 *
 * <p>A boolean would have been enough to enforce with and useless to explain with. The axis and the
 * words are what {@code /admin/access}'s <em>Simulate</em> view renders, what the refusal's status
 * code is derived from, and what a debug line prints; all three read this object rather than
 * reconstructing the reasoning from a message.
 *
 * @param granted     whether the request may proceed
 * @param reason      why not, or null where it may
 * @param words       the sentence a person reads, in the refusing axis's <em>own</em> words. Two axes
 *                    must never say the same thing: a reader told "not available" by both the ceiling
 *                    and the plan concludes the product is broken rather than that two different things
 *                    are true. Null where granted
 * @param explanation ⚠️ <strong>what the policy author wrote with {@code reason "…"}, kept as its own
 *                    field.</strong> It is also inside {@link #words}, at the front — but only as prose,
 *                    and prose is not something a client can style, translate, show on its own or leave
 *                    out. A screen that wants to lead with the author's sentence and put the mechanism
 *                    behind a disclosure triangle can only do that if the two arrive apart. Null where
 *                    the rule said nothing, which is the ordinary case
 */
public record AccessDecision(
        boolean       granted,
        RefusalReason reason,
        String        words,
        String        explanation
) {

    private static final AccessDecision ALLOWED = new AccessDecision(true, null, null, null);

    /**
     * ⚠️ Kept so that adding {@link #explanation()} left every existing construction site compiling —
     * and there are many, in four products, most of which have no explanation to give.
     */
    public AccessDecision(boolean granted, RefusalReason reason, String words) {
        this(granted, reason, words, null);
    }

    public static AccessDecision allowed() {
        return ALLOWED;
    }

    /** A refusal in the axis's own words, with nothing the policy author wrote to add. */
    public static AccessDecision refused(RefusalReason reason, String words) {
        return new AccessDecision(false, reason, words, null);
    }

    /**
     * A refusal a rule explained.
     *
     * <p>⚠️ <strong>Build {@code words} with {@link RefusalWords#explained} rather than by hand.</strong>
     * The field and the sentence are two views of one fact, and a caller that sets the field while
     * writing its own sentence produces a refusal whose two halves disagree — which is worse than one
     * that never carried the field at all.
     */
    public static AccessDecision refused(RefusalReason reason, String words, String explanation) {
        return new AccessDecision(false, reason, words, explanation);
    }

    public boolean refused() {
        return !granted;
    }

    /** Whether a rule wrote a sentence for whoever is refused. */
    public boolean isExplained() {
        return explanation != null && !explanation.isBlank();
    }

    /** Which axis answered, or null where nothing refused. */
    public AxisKind axis() {
        return reason == null ? null : reason.axis();
    }
}
