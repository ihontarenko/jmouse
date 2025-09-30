package org.jmouse.security.web.access;

import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.web.EnrichedRequestContext;
import org.jmouse.security.web.RequestMatcher;

import java.util.List;

public class AuthorizationCriterion<T> {

    private final List<RequestMatcher> matchers;
    private final T                    owner;
    private final MappingApplier<T>    applier;
    private       boolean              negate;

    public AuthorizationCriterion(MappingApplier<T> applier, T owner, List<RequestMatcher> matchers) {
        this.owner = owner;
        this.matchers = matchers;
        this.negate = false;
        this.applier = applier;
    }

    public AuthorizationCriterion<T> not() {
        this.negate = true;
        return this;
    }

    public T permitAll() {
        return access(AuthorizationPolicies.permitAll());
    }

    public T denyAll() {
        return access(AuthorizationPolicies.denyAll());
    }

    private T access(AuthorizationManager<EnrichedRequestContext> manager) {
        return applier.apply(owner, matchers, manager, negate);
    }

}
