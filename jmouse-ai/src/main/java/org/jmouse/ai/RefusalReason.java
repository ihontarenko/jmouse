package org.jmouse.ai;

/**
 * Why a call was refused, as a value that can be counted.
 *
 * <p>Carried separately from the message because the <strong>distribution</strong> of reasons is the
 * diagnostic that matters operationally, and a message cannot be grouped. A pile of
 * {@link #UNKNOWN_SCOPE} means a tool description is misleading the model about which scopes exist; a
 * pile of {@link #MISSING_PERMISSION} means a caller is configured wrongly; a pile of
 * {@link #UNPARSEABLE_VALUE} means the domain's own vocabulary is narrower than the model assumes.
 * One refusal rate hides all three, which is why the reason is part of what an
 * {@link org.jmouse.ai.spi.InvocationTrace} records rather than a note on the side.
 *
 * <p>An enum rather than an interface, unlike {@code jmouse-access}'s type of the same name. There the
 * vocabulary genuinely varies per product — one installation has plans and metering and another has
 * neither. Here it does not: every one of these is a way a <em>tool call</em> goes wrong, and none of
 * them names anything a product owns. Being able to {@code switch} over a fixed set is worth more than
 * an extensibility nobody would use.
 *
 * <p>Four reasons this deliberately does <em>not</em> carry, all of which Innoventa's version did:
 * "not an agent", "agent disabled", "owner disabled agents" and "feature disabled". Those are a
 * product's identity policy — who may become a caller at all — and they belong behind
 * {@link org.jmouse.ai.spi.CallerResolver}, which is free to raise a
 * {@link ToolRefusedException} carrying {@link #NO_CALLER} or to throw something of its own.
 */
public enum RefusalReason {

    /**
     * Nothing was authenticated, or what was authenticated may not act as a caller.
     *
     * <p>The one reason a {@link org.jmouse.ai.spi.CallerResolver} raises, and deliberately blunt: a
     * product refusing a human session token, a disabled service account and an expired credential
     * all land here, because from the mechanism's side they are one fact — there is no caller — and
     * telling them apart is exactly the kind of detail an unauthenticated client must not be told.
     */
    NO_CALLER,

    /** No action goes by that name. The refusal lists what does exist. */
    UNKNOWN_ACTION,

    /** The caller does not hold the action's required permission. */
    MISSING_PERMISSION,

    /** A scope was named that the caller cannot see. */
    UNKNOWN_SCOPE,

    /** A name matched more than one visible scope, and guessing is not on offer. */
    AMBIGUOUS_SCOPE,

    /** No scope was named and none could be determined without guessing. */
    UNDETERMINED_SCOPE,

    /** The arguments were unusable — wrong type, missing, out of range. */
    INVALID_ARGUMENT,

    /** A value did not parse, or carried a unit the domain does not recognise. */
    UNPARSEABLE_VALUE,

    /** Zero or several things matched what the call was aimed at, so the target could not be chosen. */
    AMBIGUOUS_TARGET,

    /**
     * A destructive call matched nothing.
     *
     * <p>Its own reason rather than an {@link #INVALID_ARGUMENT}, because it is the one refusal whose
     * volume says something specific: a filter that keeps matching nothing is a model working from a
     * description that promises records the domain does not hold. Folded into invalid arguments it
     * would be invisible inside the largest bucket there is.
     */
    NOTHING_TO_ACT_ON,

    /** The call would touch more records than any single call is allowed to. */
    OVER_CEILING,

    /** The caller is issuing calls faster than its bucket refills. */
    RATE_LIMITED,

    /** A confirmation token was presented that is unknown, expired, spent, or for another operation. */
    INVALID_CONFIRMATION
}
