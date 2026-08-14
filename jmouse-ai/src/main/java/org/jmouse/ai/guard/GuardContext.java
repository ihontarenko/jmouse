package org.jmouse.ai.guard;

import org.jmouse.ai.AffectedRecords;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolInvocation;

import java.util.List;
import java.util.Optional;

/**
 * One call, as it travels down the chain.
 *
 * <p>Carries the two things every guard needs — the action and the invocation — plus the three the
 * chain computes once so that no guard recomputes them: the operation's identity, the call's
 * fingerprint, and the records it would touch.
 *
 * <p><strong>The affected records are resolved lazily and exactly once.</strong> That is the whole
 * reason this is a class rather than a record. Resolving eagerly would make a read pay for a query it
 * has no use for, and — far worse — would resolve them on a call redeeming a confirmation token, where
 * the frozen set from the preview is the only correct answer and re-resolving is precisely the bug
 * two-step confirmation exists to prevent. A guard asks for them, and whether anything asked is itself
 * meaningful.
 *
 * <p>Mutable, and deliberately not shared: one instance exists per call, is visited by one thread, and
 * is discarded when the call ends.
 */
public final class GuardContext {

    private final ToolAction action;
    private final String     operationId;
    private final String     fingerprint;

    private ToolInvocation  invocation;
    private AffectedRecords affected;
    private long            reachedCount;
    private boolean         frozen;
    private boolean         throughConfirmation;

    GuardContext(ToolAction action, ToolInvocation invocation, String operationId, String fingerprint) {
        this.action      = action;
        this.invocation  = invocation;
        this.operationId = operationId;
        this.fingerprint = fingerprint;
    }

    public ToolAction action() {
        return action;
    }

    /** The call as it now stands — carrying the frozen record set, once anything has frozen one. */
    public ToolInvocation invocation() {
        return invocation;
    }

    /** Ties a preview and the call that confirms it together as one operation in a trail. */
    public String operationId() {
        return operationId;
    }

    /** What makes this call the same as another. See {@link CallFingerprint}. */
    public String fingerprint() {
        return fingerprint;
    }

    /** The confirmation token this call presented, if it presented one. */
    public Optional<String> presentedToken() {
        return invocation.optionalString(ToolInvocation.CONFIRM_ARGUMENT);
    }

    /**
     * Whether this call is redeeming a preview.
     *
     * <p>Read by three guards to decide they do not apply. A call carrying a token was ceiling-checked,
     * emptiness-checked and deduplicated when the token was issued; asking any of those again would
     * mean resolving the records a second time, which is the thing the frozen set exists to prevent.
     */
    public boolean redeemsConfirmation() {
        return presentedToken().isPresent();
    }

    /**
     * The records this call would touch, resolved once.
     *
     * <p>Memoised including the empty answer, so an action with no resolver is asked once and not per
     * guard.
     */
    public AffectedRecords affectedRecords() {
        if (affected == null) {
            affected = action.resolveAffected(invocation);
        }

        return affected;
    }

    /**
     * Hands the handler the exact records it may act on, and no way to widen them.
     *
     * <p>Called with the set a preview promised when a token is redeemed, and with the set the chain
     * resolved otherwise. A handler that re-ran its own filter could touch records the preview never
     * showed; handed the resolved set, it cannot.
     */
    public void freeze(List<AffectedRecords.Record> records, long count, boolean throughConfirmation) {
        this.invocation          = invocation.confirmedFor(records);
        this.reachedCount        = count;
        this.frozen              = true;
        this.throughConfirmation = throughConfirmation;
    }

    /**
     * Freezes whatever was resolved, if anything was and nothing has frozen a set already.
     *
     * <p>Called by the chain immediately before the work rather than by any one guard, so that a
     * handler still receives its resolved records in a product that configured the confirmation guard
     * away. Tying the freeze to one guard would make removing that guard silently change what every
     * write handler is given.
     */
    void freezeResolved() {
        if (frozen || affected == null) {
            return;
        }

        freeze(affected.records(), affected.totalCount(), false);
    }

    /** The records the call reached, for the trail. Empty where nothing ever resolved any. */
    public List<AffectedRecords.Record> reachedRecords() {
        return invocation.confirmedRecords();
    }

    /** How many it really reached; {@link #reachedRecords()} may be a capped prefix. */
    public long reachedCount() {
        return reachedCount;
    }

    /**
     * Whether this call was previewed or was the confirmation of a preview.
     *
     * <p>Exactly the set of calls whose previous state is worth keeping — and the reason a trail can
     * say what a deleted record was without keeping a copy of everything that still exists.
     */
    public boolean throughConfirmation() {
        return throughConfirmation;
    }
}
