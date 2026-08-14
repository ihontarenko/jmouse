package org.jmouse.ai.guard;

import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.RefusalReason;
import org.jmouse.ai.ToolRefusedException;

/**
 * A destructive call matching nothing is a mistake, not a no-op.
 *
 * <p>The one everybody forgets, and the reason it matters is what happens without it: the call
 * produces a preview of zero records and a token to confirm it with. A model reads that as "ready to
 * proceed" and confirms; a person reads the eventual "done" as "it worked". Nothing was destroyed and
 * nothing was accomplished, and the filter that was wrong is still wrong.
 *
 * <p>Runs before the confirmation guard for exactly that reason — the preview must never be reached.
 * The filter was wrong, and saying so is the only useful answer.
 *
 * <p>Only destructive writes. A non-destructive write matching nothing is often legitimate: an update
 * over a set that happens to be empty this minute is a no-op somebody may well have meant.
 */
public final class EmptyDestructionGuard implements InvocationGuard {

    public static final String NAME = "empty-destruction";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int order() {
        return EMPTY_DESTRUCTION_ORDER;
    }

    @Override
    public boolean appliesTo(GuardContext context) {
        return context.action().destructive() && !context.redeemsConfirmation();
    }

    @Override
    public GuardedCall guard(GuardContext context, GuardContinuation next) {
        if (!context.affectedRecords().isEmpty()) {
            return next.proceed(context);
        }

        throw new ToolRefusedException(RefusalReason.NOTHING_TO_ACT_ON, refusal(context));
    }

    /**
     * Two halves, because a call with no scope has nowhere to suggest looking instead.
     *
     * <p>An action whose records belong to a subject rather than to a place is not confined to one, and
     * telling its caller that an identifier "does not resolve in another workspace" would send a model
     * chasing an argument that action does not have.
     */
    private String refusal(GuardContext context) {
        InvocationScope scope = context.invocation().scope();

        String where = scope == null
                ? "Nothing this caller can reach matched"
                : "Nothing in the " + scope.kind() + " '" + scope.label() + "' matched";

        String advice = scope == null
                ? "the matching list action shows what is actually there."
                : "the matching list action shows what is actually there, and an identifier from one "
                + scope.kind() + " does not resolve in another.";

        return where + ", so there is nothing for '" + context.action().qualifiedName() + "' to act on. "
             + "Check the arguments naming what to affect — " + advice + " Nothing was changed.";
    }
}
