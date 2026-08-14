package org.jmouse.ai.guard;

/**
 * Something that can stop a call that is already permitted.
 *
 * <p><strong>A guard is not an authorizer.</strong> Authorization asks whether this caller may do this
 * at all; a guard asks whether <em>this particular call</em> should proceed even though they may. The
 * two vocabularies are kept apart in every name and every message here, because a refusal that
 * confuses them sends somebody to ask for a permission they already hold.
 *
 * <p>Each link decides for itself whether it applies. That is the difference between a chain and one
 * long method with the branching inlined: the question <em>"does this apply to me"</em> lives with the
 * guard that can answer it, a product adds a sixth guard by contributing one more, and a product that
 * wants none configures an empty chain and the dispatcher still works.
 *
 * <p><strong>A guard is around-advice, not a veto.</strong> Two of the shipped five replace the work
 * rather than merely permitting it — a preview stands in place of a deletion, and a suppressed
 * duplicate returns what the first call produced. A filter that could only say yes or no could express
 * neither.
 */
public interface InvocationGuard {

    /**
     * The name a product configures this guard by, and the name a startup refusal quotes.
     *
     * <p>Kebab-case, and stable: it is written in configuration files that outlive refactorings, so it
     * is not the class name and must not be derived from one.
     */
    String name();

    /**
     * Where in the chain this guard runs. Lower runs first.
     *
     * <p>Every shipped position is argued for on {@link GuardChain}, and the gaps between them are
     * deliberately wide enough that a product's own guard can sit between two without renumbering
     * anything.
     */
    int order();

    /**
     * Whether this guard has anything to say about this call.
     *
     * <p>Answered per call rather than per action, because two of the five depend on something only
     * the call knows — whether it is redeeming a confirmation token.
     */
    default boolean appliesTo(GuardContext context) {
        return true;
    }

    /**
     * Runs, and either continues down the chain or answers in its own right.
     *
     * @param next what to call to continue; not calling it means this guard has answered
     * @throws org.jmouse.ai.ToolRefusedException when the call must not proceed at all
     */
    GuardedCall guard(GuardContext context, GuardContinuation next);

    /** Cheapest, and applies to reads as well as writes, so it goes first. */
    int RATE_LIMIT_ORDER = 100;

    /** Bounds the blast radius before anything is previewed or written. */
    int CEILING_ORDER = 300;

    /** Catches a filter that matched nothing, before it becomes a preview of nothing. */
    int EMPTY_DESTRUCTION_ORDER = 400;

    /** Shows what would happen, or redeems the token that says somebody looked. */
    int CONFIRMATION_ORDER = 500;

    /** Last, so a refused call is never remembered as a completed one. */
    int DEDUPLICATION_ORDER = 600;
}
