package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.matcher.Match;
import org.jmouse.security.authorization.AuthorityPolicyAuthorizationManager;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.access.RoleHierarchy;
import org.jmouse.security.web.authorization.AuthorizationFilter;
import org.jmouse.security.web.RequestMatch;
import org.jmouse.security.web.access.ExceptionTranslationFilter;
import org.jmouse.security.web.configuration.*;
import org.jmouse.security.web.access.CompositeAuthorizationManager.Builder;
import org.jmouse.security.web.configuration.matching.AbstractAuthorizationRequestMatchCriterion;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.RouteMatch;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AuthorizeRequestConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<AuthorizeRequestConfigurer<B>, B> {

    private final AuthorizationRequestMatchCriterion requestMatchCriterion = new AuthorizationRequestMatchCriterion();

    public AuthorizationRequestMatchCriterion getRequestMatchCriterion() {
        return requestMatchCriterion;
    }

    public void addMapping(
            List<MappingMatcher<RequestRoute>> requestMatchers, AuthorizationManager<RequestMatch> manager, boolean negate) {
        requestMatchCriterion.addMapping(requestMatchers, manager, negate);
    }

    @Override
    public void initialize(B builder) throws Exception {
        if (builder instanceof AbstractConfiguredSecurityBuilder<?, ?> securityBuilder) {
            securityBuilder.getBeanContext();
        }
    }

    @Override
    public void configure(B http) {
        http.addFilterAfter(new AuthorizationFilter(
                requestMatchCriterion.createAuthorizationManager()), ExceptionTranslationFilter.class);
    }

    private String contextVariableGetter(RequestMatch context, String name) {
        Map<String, Object> variables = new HashMap<>();
        Match match = context.match();
        match.ifPresent(RouteMatch.class, m -> variables.putAll(m.variables()));
        return String.valueOf(variables.get(name));
    }

    private AuthorizationRequestMatchCriterion applyMapping(
            AuthorizationRequestMatchCriterion owner,
            List<MappingMatcher<RequestRoute>> matchers,
            AuthorizationManager<RequestMatch> manager,
            boolean negate) {
        AuthorizationManager<RequestMatch> effective = negate
                ? AuthorityPolicyAuthorizationManager.not(manager) : manager;
        addMapping(matchers, effective, negate);
        return owner;
    }

    final public class AuthorizationRequestMatchCriterion
            extends AbstractAuthorizationRequestMatchCriterion<AuthorizationAccessCriterion<AuthorizationRequestMatchCriterion, RequestMatch>> {

        private final Builder                            builder       = new Builder();
        private       List<MappingMatcher<RequestRoute>> pending;
        private       RoleHierarchy                      roleHierarchy = RoleHierarchy.none();
        private       String                             rolePrefix    = "ROLE_";

        private void validatePending() {
            if (this.pending != null) {
                throw new IllegalStateException(
                        "An incomplete mapping was found for " + this.pending
                                + ". Complete it with a terminal rule, e.g. .permitAll()/.hasRole(...)/.access(...)."
                );
            }
        }

        private void validateBuilder() {
            if (this.builder.isEmpty()) {
                throw new IllegalStateException(
                        "At least one mapping is required (e.g. authorizeHttpRequests().anyRequest().authenticated())"
                );
            }
        }

        public AuthorizationManager<HttpServletRequest> createAuthorizationManager() {
            validateBuilder();
            validatePending();
            return this.builder.build();
        }

        private void addMapping(
                List<MappingMatcher<RequestRoute>> mappingMatchers,
                AuthorizationManager<RequestMatch> manager,
                boolean negate
        ) {
            AuthorizationManager<RequestMatch> authorizationManager = negate
                    ? AuthorityPolicyAuthorizationManager.not(manager) : manager;

            for (MappingMatcher<RequestRoute> mappingMatcher : mappingMatchers) {
                builder.addMapping(mappingMatcher, authorizationManager);
            }

            this.pending = null;
        }

        @Override
        protected AuthorizationAccessCriterion<AuthorizationRequestMatchCriterion, RequestMatch> applyMatchers(
                List<MappingMatcher<RequestRoute>> requestMatchers) {
            validatePending();
            this.pending = requestMatchers;
            AuthorizeRequestConfigurer<B> outer = AuthorizeRequestConfigurer.this;
            return new AuthorizationAccessCriterion<>(
                    outer::applyMapping,
                    this,
                    requestMatchers,
                    getRoleHierarchy(),
                    outer::contextVariableGetter
            );
        }

        public AuthorizationRequestMatchCriterion roleHierarchy(RoleHierarchy hierarchy) {
            this.roleHierarchy = (hierarchy != null) ? hierarchy : RoleHierarchy.none();
            return this;
        }

        public AuthorizationRequestMatchCriterion rolePrefix(String prefix) {
            this.rolePrefix = (prefix != null) ? prefix : "ROLE_";
            return this;
        }

        public RoleHierarchy getRoleHierarchy() {
            return roleHierarchy;
        }

        public String getRolePrefix() {
            return rolePrefix;
        }

    }

}