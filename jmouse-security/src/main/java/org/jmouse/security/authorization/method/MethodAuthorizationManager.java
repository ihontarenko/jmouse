package org.jmouse.security.authorization.method;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.security.authorization.AuthorizationDecision;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.authorization.method.expression.ExpressionHandler;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.core.Authority;
import org.jmouse.security.core.method.MethodEnvelope;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class MethodAuthorizationManager implements AuthorizationManager<Method> {

    private final MethodAuthorizationRegistry registry;
    private final ExpressionHandler           expressionHandler;

    public MethodAuthorizationManager(MethodAuthorizationRegistry registry, ExpressionHandler handler) {
        this.registry = registry;
        this.expressionHandler = handler;
    }

    @Override
    public AuthorizationDecision check(Authentication authentication, Method method) {
        List<MethodAuthorizationAttribute> attributes = registry.getAttributes(method);

        if (attributes.isEmpty()) {
            return AuthorizationDecision.PERMIT;
        }

        Object[]       arguments      = (envelope instanceof MethodEnvelope methodEnvelope)
                ? methodEnvelope.getArguments() : new Object[0];

        for (MethodAuthorizationAttribute attribute : attributes) {
            switch (attribute.kind()) {
                case PRE_AUTHORIZE -> {
                    EvaluationContext evaluationContext = expressionHandler.createContext(
                            authentication, method, arguments, null);

                    if (!expressionHandler.evaluate(attribute.expression(), evaluationContext)) {
                        return AuthorizationDecision.deny("PreAuthorize denied");
                    }
                }
                case POST_AUTHORIZE -> {
                    if (authentication == null || !authentication.isAuthenticated()) {
                        return AuthorizationDecision.deny("Authentication required");
                    }

                    Set<String> authorities = authentication.getAuthorities().stream().map(Authority::authority)
                            .collect(Collectors.toSet());

                    for (String req : attribute.required()) {
                        if (!authorities.contains(req)) {
                            return AuthorizationDecision.deny("Authentication required");
                        }
                    }
                }
                default -> {}
            }
        }

        return AuthorizationDecision.permit("permitted");
    }

    /** Evaluate PostAuthorize after invocation. Returns the original result or throws if denied. */
    public Object postAuthorize(Envelope envelope, Method method, Object result) {
        List<MethodAuthorizationAttribute> attrs = registry.getAttributes(method);
        if (attrs.isEmpty()) return result;

        Authentication auth = envelope.getSecurityContext().getAuthentication().orElse(null);
        Object[] args = (envelope instanceof org.jmouse.security.method.MethodEnvelope me) ? me.getArgs() : new Object[0];

        for (MethodAuthorizationAttribute a : attrs) {
            if (a.kind() == MethodAuthorizationAttribute.Kind.POST_AUTHORIZE) {
                var ctx = expressionHandler.createContext(auth, method, args, result);
                if (!expressionHandler.evaluate(a.expression(), ctx))
                    throw new org.jmouse.security.core.exceptions.AccessDeniedException("PostAuthorize denied");
            }
        }
        return result;
    }
}
