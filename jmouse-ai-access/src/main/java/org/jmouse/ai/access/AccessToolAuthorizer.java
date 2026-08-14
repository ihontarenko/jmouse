package org.jmouse.ai.access;

import org.jmouse.access.AccessEngine;
import org.jmouse.access.Subject;
import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.spi.ToolAuthorizer;

/**
 * The tool dispatcher's authorization seam, answered by the engine that already authorizes everything
 * else.
 *
 * <p><strong>Not a parallel check that resembles the real one — the real one.</strong> A product taking
 * this module has its tools and its HTTP endpoints authorized by the same engine against the same
 * policy, so the two cannot drift; and a parallel check always eventually drifts, invisibly, until
 * something is permitted in one place and refused in the other and nobody can say which is right.
 *
 * <h2>The two questions, and why they are different questions</h2>
 *
 * <p>The dispatcher asks twice — once with no scope, then again at the scope it resolved — and the
 * difference is load-bearing. The first is <em>"may this caller do this at all"</em>, asked before any
 * scope exists so that a caller who may do nothing cannot enumerate the places by reading refusals. The
 * second is <em>"may they do it here"</em>.
 *
 * <p>⚠️ The first is therefore <strong>not</strong> {@code permits(subject, permission, installation())}
 * — that asks whether the permission is held installation-wide, which almost nobody holds, and would
 * refuse every ordinary caller before their scope was ever resolved. It is
 * {@link AccessEngine#visibilityFor}: the permission is held <em>somewhere</em>, or it is not held.
 *
 * <h2>What is knowingly not addressed</h2>
 *
 * <p>⚠️ <strong>The entitlement axis.</strong> The engine distinguishes <em>you may not</em> from
 * <em>this installation has not bought that</em> — 403 against 402 — and a tool call has no status code
 * to carry the distinction into. Both arrive here as "refused", and the dispatcher renders the second
 * as a missing permission, which will send somebody to ask for a permission that would not help.
 * Recorded rather than quietly collapsed: fixing it means a refusal reason in {@code jmouse-ai} that
 * means "not available on this plan", and that is a ticket with a real question behind it rather than a
 * line of code here.
 */
public final class AccessToolAuthorizer implements ToolAuthorizer {

    private final AccessEngine     engine;
    private final InvocationScopes scopes;

    public AccessToolAuthorizer(AccessEngine engine, InvocationScopes scopes) {
        this.engine = engine;
        this.scopes = scopes;
    }

    /**
     * Held anywhere at all.
     *
     * <p>{@code seesNothing()} is the engine's own way of saying a permission reaches no row by any
     * route — not owned, not at a place, not installation-wide — and a caller for whom that is true
     * cannot be permitted this action in any scope, so there is no point resolving one.
     */
    @Override
    public boolean permits(CallerIdentity caller, ToolAction action) {
        return !engine.visibilityFor(subjectOf(caller), action.requiredPermission()).seesNothing();
    }

    /** Held here. The ordinary question, asked once the dispatcher knows where "here" is. */
    @Override
    public boolean permitsInScope(CallerIdentity caller, ToolAction action, InvocationScope scope) {
        return engine.permits(
                subjectOf(caller), action.requiredPermission(), scopes.targetOf(scope));
    }

    /**
     * Who the engine decides about.
     *
     * <p>The one place the two identity models meet, and they meet cleanly: a caller acting on behalf
     * of somebody is exactly the engine's <em>service sub-account</em>, whose ceiling is its master's in
     * every scope. So authorization rests on the caller and ownership on the master, which is what both
     * mechanisms already meant separately.
     *
     * <p>⚠️ Private, and staying that way. This is the bridge's own reading of a caller; exposing it
     * would invite a product to build a {@code Subject} here and pass it somewhere else, and then the
     * translation would live in two places.
     */
    private static Subject subjectOf(CallerIdentity caller) {
        return caller.actsForItself()
                ? Subject.of(caller.callerId(), caller.describe())
                : Subject.agent(caller.callerId(), caller.actsOnBehalfOfId(), caller.describe());
    }
}
