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
 * cannot use. What an implementation contributes to the words is deliberately narrow and deliberately
 * not nothing: which permission to name ({@link #unmetPermission}) and why it is missing
 * ({@link #refusalAdvice}).
 *
 * <p>⚠️ <strong>Which permission to name is still the implementation's to say.</strong> An authorizer
 * weighing more than one — a second axis governing whether an action may be reached through a tool at
 * all, say — knows which one actually failed, and the dispatcher cannot. Left to
 * {@link ToolAction#requiredPermission()} the refusal named a permission the caller already held, and
 * sent whoever read it to grant it again. See {@link #unmetPermission}.
 */
public interface ToolAuthorizer {

    /** May this caller run this action anywhere at all? Asked before any scope exists. */
    boolean permits(CallerIdentity caller, ToolAction action);

    /** …and here? Asked after the scope resolved, for a scope-confined action only. */
    default boolean permitsInScope(CallerIdentity caller, ToolAction action, InvocationScope scope) {
        return permits(caller, action);
    }

    /**
     * Which permission the refusal should name — asked only after one of the two above said no.
     *
     * <p>An authorizer that weighs exactly one permission wants the default and should leave this
     * alone. One that weighs several has to override it, because naming the wrong one is worse than
     * naming none: an operator reads it, finds the caller already holds what it names, and concludes
     * the engine is broken.
     *
     * <p>On the refusal path only, so re-asking the engine to find out which answer was no costs
     * nothing a caller waits for.
     *
     * @param scope where the question was asked, or null for the gate that runs before any scope is
     *              resolved — the two can fail over different permissions and an override should say so
     */
    default String unmetPermission(CallerIdentity caller, ToolAction action, InvocationScope scope) {
        return action.requiredPermission();
    }

    /**
     * One more sentence for the refusal — <em>why</em> the permission is missing, when the product knows
     * and the library cannot.
     *
     * <p>⚠️ <strong>A refusal that names a permission reads as a broken grant, and that is frequently
     * not what happened.</strong> Naming the permission is necessary and it is not sufficient: a product
     * whose permissions are handed out by belonging somewhere refuses a caller who belongs nowhere with
     * a sentence indistinguishable from one aimed at a caller whose role was misconfigured. The reader —
     * an operator, or a model reporting to one — goes and grants a permission that no assignment could
     * have carried, concludes the engine is broken, and says so. Tessera hit exactly this on every
     * freshly created database.
     *
     * <p>This does <em>not</em> reopen the message to five implementations each writing their own
     * sentence. The library still composes the refusal, still names the permission, and still says it in
     * the same words everywhere; this appends a diagnosis to it. Say what the state is and what would
     * change it, in one or two sentences.
     *
     * <p>⚠️ Answer null whenever there is nothing specific to add — an override guessing at a cause it
     * has not established is worse than the plain refusal, which is at least true.
     *
     * <p>On the refusal path only, like {@link #unmetPermission}, so an implementation may query for its
     * answer without anybody waiting on it during a permitted call.
     *
     * @param scope where the question was asked, or null for the gate that runs before any scope is
     *              resolved
     * @return an extra sentence to append, or null for the refusal as the library wrote it
     */
    default String refusalAdvice(CallerIdentity caller, ToolAction action, InvocationScope scope) {
        return null;
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
