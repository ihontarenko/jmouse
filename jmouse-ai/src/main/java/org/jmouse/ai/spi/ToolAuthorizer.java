package org.jmouse.ai.spi;

import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.ToolAction;

/**
 * May this caller run this action?
 *
 * <p>An interface here rather than a dependency on an authorization engine, and that is what lets a
 * product have tools without having an access model at all. A product that holds both takes
 * {@code jmouse-ai-access} and gets its tool calls and its HTTP endpoints authorized by one engine
 * against one policy — rather than by a parallel one that drifts.
 *
 * <p><strong>Asked twice, and the two questions are not the same question.</strong> The ordering of
 * the dispatch requires it:
 *
 * <ul>
 *   <li>{@link #permits} runs <em>before any scope is resolved</em>. Resolving the scope first would
 *       let a caller who may read nothing discover which scopes exist by reading the refusals — the
 *       refusal for an unknown scope names the ones that are visible, which is the disclosure the
 *       ordering exists to prevent.
 *   <li>{@link #permitsInScope} runs after resolution, and only for a scope-confined action. Without
 *       it, a product whose permissions are held <em>at places</em> could only ever ask its engine the
 *       weaker question, and a caller permitted somewhere would be permitted everywhere.
 * </ul>
 *
 * <p>The second defaults to the first, so a product whose permissions are not scoped implements one
 * method and never thinks about this again.
 *
 * <p>Answers a boolean rather than a decision carrying a message. The refusal is composed once, by the
 * dispatcher, in the words a model can act on and naming the permission that was missing — five
 * implementations each writing their own sentence is five chances for one to say something a caller
 * cannot use.
 */
public interface ToolAuthorizer {

    /** May this caller run this action anywhere at all? Asked before any scope exists. */
    boolean permits(CallerIdentity caller, ToolAction action);

    /** …and here? Asked after the scope resolved, for a scope-confined action only. */
    default boolean permitsInScope(CallerIdentity caller, ToolAction action, InvocationScope scope) {
        return permits(caller, action);
    }

    /**
     * Permits everything.
     *
     * <p>The default, because a product must be able to run a tool before it has decided any of this,
     * and because a library that refused by default would be refusing on evidence it does not have.
     *
     * <p>⚠️ Note what still holds with this in place: the catalogue has already refused to start if any
     * action failed to declare a permission. So an application running on this default has an
     * <em>unenforced</em> declaration rather than a missing one, and turning it into an enforced one is
     * a matter of implementing one method — not of going back through every tool.
     */
    static ToolAuthorizer permitAll() {
        return (caller, action) -> true;
    }
}
