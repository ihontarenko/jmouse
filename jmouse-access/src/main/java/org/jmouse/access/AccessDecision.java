package org.jmouse.access;

/**
 * The answer to "may I", and — where the answer is no — which of the five axes said so.
 *
 * <p>A boolean would have been enough to enforce with and useless to explain with. The axis and the
 * words are what {@code /admin/access}'s <em>Simulate</em> view renders, what the refusal's status
 * code is derived from, and what a debug line prints; all three read this object rather than
 * reconstructing the reasoning from a message.
 *
 * @param granted whether the request may proceed
 * @param reason  why not, or null where it may
 * @param words   the sentence a person reads, in the refusing axis's <em>own</em> words. Two axes
 *                must never say the same thing: a reader told "not available" by both the ceiling and
 *                the plan concludes the product is broken rather than that two different things are
 *                true. Null where granted
 */
public record AccessDecision(
        boolean       granted,
        RefusalReason reason,
        String        words
) {

    private static final AccessDecision ALLOWED = new AccessDecision(true, null, null);

    public static AccessDecision allowed() {
        return ALLOWED;
    }

    public static AccessDecision refused(RefusalReason reason, String words) {
        return new AccessDecision(false, reason, words);
    }

    public boolean refused() {
        return !granted;
    }

    /** Which axis answered, or null where nothing refused. */
    public AxisKind axis() {
        return reason == null ? null : reason.axis();
    }
}
