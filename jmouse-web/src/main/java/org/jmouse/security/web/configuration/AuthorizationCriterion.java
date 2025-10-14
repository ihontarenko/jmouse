package org.jmouse.security.web.configuration;

import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorityPolicyAuthorizationManager;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.access.MappingApplier;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.List;
import java.util.function.Function;

public class AuthorizationCriterion<T, C> {

    private final List<MappingMatcher> matchers;
    private final T                    owner;
    private final MappingApplier<T, C> applier;
    private final ContextVariables<C>  variables;
    private       boolean              negate = false;

    public AuthorizationCriterion(
            MappingApplier<T, C> applier,
            T owner,
            List<MappingMatcher> matchers,
            ContextVariables<C> variables
    ) {
        this.applier = applier;
        this.owner = owner;
        this.variables = variables;
        this.matchers = matchers;
    }

    public AuthorizationCriterion<T, C> not() {
        this.negate = true;
        return this;
    }

    public T permitAll() {
        return access(AuthorityPolicyAuthorizationManager.permitAll());
    }

    public T denyAll() {
        return access(AuthorityPolicyAuthorizationManager.denyAll());
    }

    public T anonymous() {
        return access(AuthorityPolicyAuthorizationManager.anonymous());
    }

    public T authenticated() {
        return access(AuthorityPolicyAuthorizationManager.authenticated());
    }

    public T hasAuthority(String authority) {
        return access(AuthorityPolicyAuthorizationManager.hasAuthority(authority));
    }

    public T hasAnyAuthority(String... authorities) {
        return access(AuthorityPolicyAuthorizationManager.hasAnyAuthority(authorities));
    }

    public T hasRole(String role) {
        return access(AuthorityPolicyAuthorizationManager.hasRole(role));
    }

    public T hasAnyRole(String... roles) {
        return access(AuthorityPolicyAuthorizationManager.hasAnyRole(roles));
    }

    public AuthorizedVariable hasVariable(String variable) {
        return new AuthorizedVariable(variable);
    }

    public T access(AuthorizationManager<C> manager) {
        return applier.apply(owner, matchers, manager, negate);
    }

    public final class AuthorizedVariable {

        private final String variable;

        private AuthorizedVariable(String variable) { this.variable = variable; }

        public T equalTo(Function<Authentication, String> extractor) {
            return access((authentication, context) -> {
                String  expected  = (authentication == null) ? null : extractor.apply(authentication);
                return (expected != null && expected.equals(variables.getValue(context, variable)))
                        ? AccessResult.PERMIT : AccessResult.DENY;
            });
        }

        public T equalToValue(String expected) {
            return access((authentication, context)
                    -> expected != null && expected.equals(variables.getValue(context, variable))
                        ? AccessResult.PERMIT : AccessResult.DENY);
        }
    }

}
