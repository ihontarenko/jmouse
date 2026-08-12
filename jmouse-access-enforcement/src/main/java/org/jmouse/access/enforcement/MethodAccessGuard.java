package org.jmouse.access.enforcement;

import org.jmouse.access.AccessDecision;
import org.jmouse.access.AccessEngine;
import org.jmouse.access.AccessTarget;
import org.jmouse.access.EngineRefusals;
import org.jmouse.access.Subject;
import org.jmouse.access.spi.AccessContextScope;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * One method call, checked — the whole of enforcement, with no framework anywhere in it.
 *
 * <p>Give it a method, the class it was called on, the arguments and who is asking; get back a
 * refusal to act on, or nothing. What to <em>do</em> with a refusal is the caller's: a Spring
 * interceptor throws, a jMouse-web filter writes a response, a test asserts on it, and a shadow mode
 * logs it and proceeds. None of those belong to the decision.
 *
 * <p><strong>Composition rather than inheritance, deliberately.</strong> The obvious shape is an
 * abstract class an interceptor extends, and it is the wrong one: a framework's interceptor already
 * has a supertype it must extend, and inheriting from this would spend the one extension slot on
 * plumbing. Every adapter can hold one of these; not every adapter can be one.
 *
 * <p><strong>A declaration that resolves to no row refuses.</strong> Not because the caller is
 * necessarily wrong, but because the alternative — reading an unresolvable identifier as an unscoped
 * call — passes every axis that is about a place. That is one typo away from an open door, and it is
 * the failure this class must not have.
 *
 * <h2>⚠️ Two entry points, and which one an adapter wants</h2>
 *
 * <p>{@link #around} is the one. It publishes what {@link AccessContext} declares, asks the engine,
 * and hands back a window the adapter closes <em>after the method has run</em> — so a programmatic
 * check three layers down sees the same action the annotation published. Ordering is the whole point:
 * a value attached any later cannot influence a gate that has already opened.
 *
 * <p>{@link #check} is the older, simpler shape and publishes nothing that outlives it. Correct for a
 * caller with no {@code @AccessContext} anywhere; wrong the moment one appears, because the window
 * closes before the method body it was meant to cover.
 */
public class MethodAccessGuard {

    private final AccessEngine        engine;
    private final AccessRequirements  requirements;
    private final AccessTargetBinding binding;
    private final EngineRefusals      refusals;

    private final AccessContextDeclarations contexts;
    private final AccessContextScope        published;

    /**
     * @param contexts  what a route publishes about itself, or null where nothing does
     * @param published where a publication lives while a call runs, or null for a caller that
     *                  publishes nothing
     */
    public MethodAccessGuard(
            AccessEngine              engine,
            AccessRequirements        requirements,
            AccessTargetBinding       binding,
            EngineRefusals            refusals,
            AccessContextDeclarations contexts,
            AccessContextScope        published) {

        this.engine       = engine;
        this.requirements = requirements;
        this.binding      = binding;
        this.refusals     = refusals;
        this.contexts     = contexts;
        this.published    = published == null ? AccessContextScope.none() : published;
    }

    /** The wiring for a product that declares no actions. */
    public MethodAccessGuard(
            AccessEngine        engine,
            AccessRequirements  requirements,
            AccessTargetBinding binding,
            EngineRefusals      refusals) {

        this(engine, requirements, binding, refusals, null, null);
    }

    /**
     * Publishes what the route says it is doing, checks the call, and keeps the publication open
     * until the returned window is closed.
     *
     * <p>Used as
     *
     * <pre>{@code
     * try (GuardedCall call = guard.around(method, targetClass, arguments, subject)) {
     *     call.refusal().ifPresent(refusal -> handler.onRefusal(refusal, method));
     *     return invocation.proceed();
     * }
     * }</pre>
     *
     * <p>⚠️ <strong>The window has to close, and it has to close in a {@code finally}.</strong> An
     * exception on the way out of the method must not leave a stale action on the stack for whatever
     * runs next on that thread — which is what {@code try}-with-resources is for and why this hands
     * back an {@link AutoCloseable} rather than trusting anybody to remember.
     */
    public GuardedCall around(
            Method method, Class<?> targetClass, Object[] arguments, Subject subject) {

        AccessContextDeclarations.Declared declared = contexts == null
                ? null
                : contexts.of(method, targetClass).orElse(null);

        if (declared == null) {
            return new GuardedCall(check(method, targetClass, arguments, subject).orElse(null), null);
        }

        AccessContextDeclarations.Published values = declared.publish(arguments);

        if (values.isRefused()) {
            return new GuardedCall(brokenPromise(method, targetClass, values.refusal()), null);
        }

        AutoCloseable window = published.push(declared.action(), values.values());

        try {
            return new GuardedCall(check(method, targetClass, arguments, subject).orElse(null), window);
        } catch (RuntimeException | Error failed) {
            new GuardedCall(null, window).close();
            throw failed;
        }
    }

    /**
     * Why this call may not proceed, or nothing where it may.
     *
     * <p>A method that declares nothing is not this class's business and returns empty — whether an
     * undeclared route is allowed to exist at all is a question for a startup check, where it can be
     * answered once for the whole application instead of once per request.
     *
     * <p>⚠️ It publishes nothing. See {@link #around}.
     */
    public Optional<AccessRefusal> check(
            Method method, Class<?> targetClass, Object[] arguments, Subject subject) {

        Optional<AccessRequirement> declared = requirements.of(method, targetClass);

        if (declared.isEmpty()) {
            return Optional.empty();
        }

        AccessRequirement      required = declared.get();
        Optional<AccessTarget> aimed    = binding.bind(method, arguments, required, subject);

        if (aimed.isEmpty()) {
            return Optional.of(new AccessRefusal(required, null, AccessDecision.refused(
                    refusals.noSuchRow(),
                    "No such " + required.resource().getSimpleName().toLowerCase() + ".")));
        }

        AccessTarget   target   = aimed.get();
        AccessDecision decision = engine.decide(subject, required.permission(), target);

        return decision.refused()
                ? Optional.of(new AccessRefusal(required, target, decision))
                : Optional.empty();
    }

    /**
     * A route that promised a value and was called without it.
     *
     * <p>⚠️ <strong>Only where the route is also guarded.</strong> A method carrying
     * {@link AccessContext} and no {@link RequiresAccess} is not one this class gates, and refusing it
     * would be the guard inventing authority it was not given. That combination is a declaration
     * smell — a route publishing for rules that can never be reached — and belongs to a startup check,
     * which is where it can be reported for every route at once.
     */
    private AccessRefusal brokenPromise(Method method, Class<?> targetClass, String why) {
        return requirements.of(method, targetClass)
                .map(required -> new AccessRefusal(
                        required, null, AccessDecision.refused(refusals.undeclaredValue(), why)))
                .orElse(null);
    }

    /**
     * A checked call and the publication that outlives it.
     *
     * <p>⚠️ Closing is not optional and closing twice is harmless. What must not happen is closing
     * <em>before</em> the method runs: everything the publication exists for happens inside it.
     */
    public static final class GuardedCall implements AutoCloseable {

        private final AccessRefusal refusal;
        private AutoCloseable       window;

        private GuardedCall(AccessRefusal refusal, AutoCloseable window) {
            this.refusal = refusal;
            this.window  = window;
        }

        /** Why the call may not proceed, or nothing where it may. */
        public Optional<AccessRefusal> refusal() {
            return Optional.ofNullable(refusal);
        }

        @Override
        public void close() {
            AutoCloseable open = window;

            window = null;

            if (open == null) {
                return;
            }

            try {
                open.close();
            } catch (Exception impossible) {
                // The only implementation restores a stack frame and throws nothing an adapter can
                // act on. Wrapping keeps this off every caller's signature.
                throw new IllegalStateException(
                        "An access-context window could not be closed: " + impossible.getMessage(),
                        impossible);
            }
        }
    }
}
