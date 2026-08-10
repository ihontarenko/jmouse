package org.jmouse.access.enforcement;

import org.jmouse.access.AccessDecision;
import org.jmouse.access.AccessEngine;
import org.jmouse.access.AccessTarget;
import org.jmouse.access.EngineRefusals;
import org.jmouse.access.Subject;

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
 */
public class MethodAccessGuard {

    private final AccessEngine        engine;
    private final AccessRequirements  requirements;
    private final AccessTargetBinding binding;
    private final EngineRefusals      refusals;

    public MethodAccessGuard(
            AccessEngine        engine,
            AccessRequirements  requirements,
            AccessTargetBinding binding,
            EngineRefusals      refusals) {

        this.engine       = engine;
        this.requirements = requirements;
        this.binding      = binding;
        this.refusals     = refusals;
    }

    /**
     * Why this call may not proceed, or nothing where it may.
     *
     * <p>A method that declares nothing is not this class's business and returns empty — whether an
     * undeclared route is allowed to exist at all is a question for a startup check, where it can be
     * answered once for the whole application instead of once per request.
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
}
