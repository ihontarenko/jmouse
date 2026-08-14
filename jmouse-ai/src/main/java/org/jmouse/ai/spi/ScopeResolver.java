package org.jmouse.ai.spi;

import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.RefusalReason;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolRefusedException;

/**
 * Turns the place a caller <em>named</em> into the scope an action runs in.
 *
 * <p>Addressed by name rather than by identifier, and that is worth stating: a model sees names in
 * conversation and never sees identifiers, so an identifier argument is an invitation to invent one.
 *
 * <p>Two rules that every implementation should keep, because they are what makes the refusals useful:
 *
 * <ul>
 *   <li><strong>Never guess.</strong> Anything that could resolve more than one way is refused with
 *       {@link RefusalReason#AMBIGUOUS_SCOPE}, and the refusal lists the candidates so the next call
 *       can be right. Two places of one name, both visible, and picking either silently is how work
 *       lands in the wrong one — a discovery that is neither immediate nor pleasant.
 *   <li><strong>Always name what is visible.</strong> A refusal saying only "no such workspace" leaves
 *       a model to guess again; one that lists what the caller can see ends the guessing in one round.
 * </ul>
 *
 * <p>A single visible place <em>is</em> the default, unambiguously. More than one is not a default at
 * all and should be refused with {@link RefusalReason#UNDETERMINED_SCOPE} rather than picked — the
 * whole reason the scope is an argument is that silently choosing the wrong one is expensive and
 * invisible. Mark whichever way it resolved with {@link InvocationScope#defaulted}, because that flag
 * travels into the response and is what makes the echo worth reading.
 *
 * <p>⚠️ <strong>This is also where a product publishes the resolved scope into whatever thread-bound
 * state its own services read.</strong> A service written for a request carrying a header has no header
 * here, and this is the first thing to know the answer. Publishing it here rather than through a
 * callback keeps the side effect in the product's own code, where it can also be cleared.
 */
@FunctionalInterface
public interface ScopeResolver {

    /**
     * @param requestedScope the scope argument as given, or null when it was omitted
     * @throws ToolRefusedException when the name matches nothing, matches several, or nothing was named
     *                              and there is no single default
     */
    InvocationScope resolve(CallerIdentity caller, ToolAction action, String requestedScope);

    /**
     * ⚠️ <strong>Refuses, unlike every other seam's default.</strong>
     *
     * <p>A missing authorizer is a product that has not got to authorization yet; a missing scope
     * resolver on a scope-confined action is a call that would run <em>somewhere undetermined</em>,
     * which is worse than not running. There is no permissive answer to give — "no scope" is not a
     * scope, and inventing one would put the work somewhere nobody chose.
     *
     * <p>The refusal names the action, because the mistake is nearly always that one action was marked
     * scope-confined in a product that has none.
     */
    static ScopeResolver refusing() {
        return (caller, action, requestedScope) -> {
            throw new ToolRefusedException(RefusalReason.UNDETERMINED_SCOPE,
                    "'" + action.qualifiedName() + "' is declared as running inside a scope, and this "
                    + "application has no way to resolve one. Either supply a ScopeResolver, or drop "
                    + "the scope-confined flag from the action. Nothing was changed.");
        };
    }
}
