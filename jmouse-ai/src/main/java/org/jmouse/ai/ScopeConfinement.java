package org.jmouse.ai;

/**
 * <em>"Is this record inside the scope this call is acting in?"</em>
 *
 * <p>Every single-record read reaches a service whose own check asks a different question — whether
 * the <em>subject</em> may read the thing. That is the right question for a user interface, where a
 * person is looking at their own data, and the wrong one here: a caller is confined to a subset of
 * the subject's places, so an identifier from outside that subset would otherwise resolve straight
 * through the boundary the whole feature is scoped by.
 *
 * <p><strong>How membership is established differs per domain and stays with the caller</strong> — an
 * identifier comparison for one thing, a scan of the visible list for another, a join for a third.
 * What is shared is the refusal, because four hand-written versions of one sentence are four chances
 * for one of them to say something a model cannot act on.
 *
 * <p>This is confinement, not authorization. Whether the caller may run the action at all was settled
 * before the handler was reached; this asks only whether the <em>thing named</em> is somewhere the
 * call is entitled to look.
 */
public final class ScopeConfinement {

    private ScopeConfinement() {
    }

    /**
     * @param invocation  the call in progress, for the scope it resolved to
     * @param inThisScope what the caller determined, in whatever way its domain allows
     * @param kind        what was being looked for, in the user's words: {@code "project"}, {@code "page"}
     * @param identifier  what was named, so the refusal can quote it back
     */
    public static void require(
            ToolInvocation invocation, boolean inThisScope, String kind, String identifier) {

        if (inThisScope) {
            return;
        }

        throw new ToolRefusedException(RefusalReason.INVALID_ARGUMENT, refusal(invocation, kind, identifier));
    }

    /**
     * Two sentences, because a call with no scope has nowhere to suggest looking instead.
     *
     * <p>An action whose records belong to a subject rather than to a place is not confined to one,
     * and telling its caller to "name another workspace" would send a model chasing an argument that
     * does not exist on that action.
     */
    private static String refusal(ToolInvocation invocation, String kind, String identifier) {
        InvocationScope scope = invocation.scope();

        if (scope == null) {
            return "No " + kind + " '" + identifier + "' is visible to this caller. List what is "
                 + "actually there first — an identifier that belongs to somebody else does not "
                 + "resolve here.";
        }

        return "No " + kind + " '" + identifier + "' is visible in the " + scope.kind() + " '"
             + scope.label() + "'. It may belong to another " + scope.kind() + " — name that one in "
             + "the '" + ToolInvocation.SCOPE_ARGUMENT + "' argument if this caller can see it, or "
             + "list what is actually there first.";
    }
}
