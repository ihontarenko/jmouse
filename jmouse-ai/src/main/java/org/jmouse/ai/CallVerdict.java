package org.jmouse.ai;

/**
 * What happened to a call that was permitted.
 *
 * <p>Three outcomes, and they are not interchangeable to anyone reading a trail afterwards: a preview
 * changed nothing and may still be confirmed, a suppressed duplicate changed nothing because an
 * identical call already had, and a carried-out call changed something. Two of the three mean
 * <em>nothing happened</em>, and a client that renders all three the same way tells a user their work
 * is done when it is not.
 *
 * <p>Lives here rather than in {@code org.jmouse.ai.guard} because two types carry it — what the guard
 * chain produced, and what the dispatcher answers with — and the second must not have to import the
 * first's package to name its own outcome.
 *
 * <p>A refusal is <strong>not</strong> in this list, and neither is a failure. Both leave through an
 * exception rather than a return value, because a caller that has to check a verdict before trusting a
 * payload will eventually forget to.
 */
public enum CallVerdict {

    /** The handler ran. */
    CARRIED_OUT,

    /** A preview was returned in place of the work, with a token to confirm it. */
    PREVIEWED,

    /** An identical call moments earlier had already done it, and its result is what came back. */
    DUPLICATE_SUPPRESSED;

    /** Whether anything actually changed — the question a transport asks before saying "done". */
    public boolean changedSomething() {
        return this == CARRIED_OUT;
    }
}
