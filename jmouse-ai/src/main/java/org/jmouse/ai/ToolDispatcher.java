package org.jmouse.ai;

import org.jmouse.ai.guard.GuardChain;
import org.jmouse.ai.guard.GuardedCall;
import org.jmouse.ai.spi.CallerResolver;
import org.jmouse.ai.spi.InvocationTrace;
import org.jmouse.ai.spi.ScopeResolver;
import org.jmouse.ai.spi.ToolAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The only way to reach a handler.
 *
 * <p>Everything worth observing about this mechanism is observable here, at
 * {@code (caller, action, arguments) -> outcome}: permission refusals, scope resolution and its echo,
 * argument validation, every guard. An in-app assistant, a Model Context Protocol server and anything
 * else are transports over this one method, and none of them is a second implementation of it.
 *
 * <p><strong>The permission check here is not belt-and-braces; it is very likely the only
 * authorization that runs.</strong> A handler calls a domain service directly, bypassing whatever a
 * product hangs on its HTTP layer, and nothing downstream of this method will check anything.
 *
 * <h2>The order, and why each position is load-bearing</h2>
 *
 * <ol>
 *   <li><strong>Identity.</strong> No caller, no call.
 *   <li><strong>Existence.</strong> An unknown name is refused with the list of what does exist, and
 *       is <em>counted</em> — a client calling a name that is not there means a stale tool list or a
 *       hallucinated one, and it is otherwise the single failure an operator cannot see.
 *   <li><strong>Permission</strong>, asked without a scope.
 *   <li><strong>Scope</strong>, for a scope-confined action, and then the permission again <em>at</em>
 *       it.
 *   <li><strong>Guards</strong> — the whole chain. Which of them apply is the chain's decision, never
 *       a branch here.
 *   <li><strong>The work.</strong>
 * </ol>
 *
 * <p>⚠️ <strong>Two re-orderings are bugs that look like refactors.</strong> Resolving the scope before
 * checking the permission would let a caller who may read nothing discover which scopes exist by
 * reading the refusals — an unknown-scope refusal names the visible ones, deliberately, because that
 * is what makes it useful to a permitted caller. And running the guards before resolving the scope
 * would leave them counting affected records in a scope that was never determined.
 *
 * <p>The scope is read before the {@code try} and reassigned inside it, so that a refusal raised after
 * resolution is recorded against the scope it was actually about.
 *
 * <p>Opens no transaction. Wrapping every call in one ambient transaction would quietly decide the
 * transaction boundary for every write action a product will ever add, which is a decision that
 * belongs to the handler.
 */
public final class ToolDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolDispatcher.class);

    private final ToolCatalog     catalog;
    private final CallerResolver  callerResolver;
    private final ToolAuthorizer  authorizer;
    private final ScopeResolver   scopeResolver;
    private final GuardChain      guards;
    private final InvocationTrace trace;

    public ToolDispatcher(
            ToolCatalog     catalog,
            CallerResolver  callerResolver,
            ToolAuthorizer  authorizer,
            ScopeResolver   scopeResolver,
            GuardChain      guards,
            InvocationTrace trace) {

        this.catalog        = catalog;
        this.callerResolver = callerResolver;
        this.authorizer     = authorizer;
        this.scopeResolver  = scopeResolver;
        this.guards         = guards;
        this.trace          = trace;
    }

    /**
     * A dispatcher over a catalogue and nothing else — every seam at its default.
     *
     * <p>What ticket 02 promises and what a first draft wants: a tool can be dispatched with no seam
     * implemented. Note which default is <em>not</em> permissive — {@link ScopeResolver#refusing()} —
     * and why, on that method.
     */
    public static ToolDispatcher over(ToolCatalog catalog) {
        return new ToolDispatcher(
                catalog,
                CallerResolver.anonymous(),
                ToolAuthorizer.permitAll(),
                ScopeResolver.refusing(),
                GuardChain.defaults(),
                InvocationTrace.none());
    }

    /**
     * Runs one action.
     *
     * @param publishedName the action as the client named it, e.g. {@code entries_list}
     * @param arguments     the arguments as sent; null is read as none
     * @throws ToolRefusedException when the call is refused, with a message written for the caller
     */
    public ToolOutcome dispatch(String publishedName, Map<String, Object> arguments) {
        CallerIdentity caller = requireCaller();
        ToolAction     action = requireAction(caller, publishedName);

        InvocationScope scope = null;

        try {
            requirePermission(caller, action);

            scope = resolveScope(caller, action, arguments);

            ToolInvocation invocation = new ToolInvocation(caller, scope, arguments);
            GuardedCall    guarded    = guards.run(
                    action, invocation, guardedInvocation -> invoke(action, guardedInvocation));

            // After the work, so a refused or failed call is neither recorded as activity nor counted
            // as one that did something.
            trace.recordOutcome(caller, scope, action, guarded);

            LOGGER.info("{} by {} in {}",
                    action.qualifiedName(), caller.describe(),
                    scope == null ? "-" : scope.id());

            return new ToolOutcome(guarded.payload(), scope, guarded.verdict(), guarded.operationId());

        } catch (ToolRefusedException refusal) {
            trace.recordRefusal(caller, scope, action, refusal);
            throw refusal;

        } catch (RuntimeException failure) {
            // A refusal is a decision and nothing was attempted. This is the other case: the work
            // reached the domain and stopped somewhere inside it, possibly having done part of what it
            // was asked. Recording it is the only thing that would ever prompt anyone to look.
            trace.recordFailure(caller, scope, action, failure);
            throw failure;
        }
    }

    /** Every action, as much of it as the asker is allowed to see. Never a handler. */
    public ToolCatalog catalog() {
        return catalog;
    }

    // ── The steps ────────────────────────────────────────────────────────────────

    private CallerIdentity requireCaller() {
        CallerIdentity caller = callerResolver.resolve();

        if (caller != null) {
            return caller;
        }

        throw new ToolRefusedException(RefusalReason.NO_CALLER,
                "Nothing was authenticated, so there is nobody to run this as. Connect with a "
                + "credential this application accepts and try again.");
    }

    /**
     * Counted against the caller rather than against an action, because there is no action: the name
     * matched nothing.
     */
    private ToolAction requireAction(CallerIdentity caller, String publishedName) {
        return catalog.find(publishedName).orElseThrow(() -> {
            trace.recordUnknownAction(caller, publishedName);

            return new ToolRefusedException(RefusalReason.UNKNOWN_ACTION,
                    "There is no action called '" + publishedName + "'. Available: "
                    + String.join(", ", catalog.publishedNames()) + ".");
        });
    }

    /**
     * The gate, asked without a scope.
     *
     * <p>The message says plainly that a permission is missing and names which one. A refusal that
     * blamed anything else would have the client report a different problem to the user, and a
     * permission refusal that is not unambiguous is worse than none.
     */
    private void requirePermission(CallerIdentity caller, ToolAction action) {
        if (authorizer.permits(caller, action)) {
            return;
        }

        LOGGER.info("Refused {} for {} — missing {}",
                action.qualifiedName(), caller.describe(), action.requiredPermission());

        throw new ToolRefusedException(RefusalReason.MISSING_PERMISSION,
                "This caller is not allowed to do that. '" + action.qualifiedName() + "' needs the '"
                + action.requiredPermission() + "' permission and this caller does not hold it.");
    }

    /** The same gate again, now that there is somewhere to ask about. See {@link ToolAuthorizer}. */
    private void requirePermissionInScope(
            CallerIdentity caller, ToolAction action, InvocationScope scope) {

        if (authorizer.permitsInScope(caller, action, scope)) {
            return;
        }

        LOGGER.info("Refused {} for {} in {} — missing {} there",
                action.qualifiedName(), caller.describe(), scope.id(), action.requiredPermission());

        throw new ToolRefusedException(RefusalReason.MISSING_PERMISSION,
                "This caller holds '" + action.requiredPermission() + "' somewhere, but not in the "
                + scope.kind() + " '" + scope.label() + "'. Name a different " + scope.kind()
                + " in the '" + ToolInvocation.SCOPE_ARGUMENT + "' argument, or ask for the permission "
                + "there.");
    }

    private InvocationScope resolveScope(
            CallerIdentity caller, ToolAction action, Map<String, Object> arguments) {

        if (!action.scopeConfined()) {
            return null;
        }

        Object requested = arguments == null ? null : arguments.get(ToolInvocation.SCOPE_ARGUMENT);

        if (requested != null && !(requested instanceof String)) {
            throw new ToolRefusedException(RefusalReason.INVALID_ARGUMENT,
                    "'" + ToolInvocation.SCOPE_ARGUMENT + "' must be a name, as text.");
        }

        InvocationScope scope = scopeResolver.resolve(caller, action, (String) requested);

        requirePermissionInScope(caller, action, scope);

        return scope;
    }

    private Object invoke(ToolAction action, ToolInvocation invocation) {
        try {
            return action.handler().apply(invocation);
        } catch (ToolRefusedException refusal) {
            throw refusal;
        } catch (RuntimeException failure) {
            // A domain exception's message is often written for a person and worth passing on; what
            // must not happen is a stack trace reaching the client as an unexplained failure.
            LOGGER.warn("{} failed: {}", action.qualifiedName(), failure.getMessage(), failure);
            throw failure;
        }
    }
}
