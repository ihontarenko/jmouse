package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authorization.AuthorityPolicyAuthorizationManager;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.access.RoleHierarchy;
import org.jmouse.security.web.AuthorizationFilter;
import org.jmouse.security.web.RequestSecurityContext;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.configuration.*;
import org.jmouse.security.web.access.DelegatingAuthorizationManager.Builder;

import java.util.List;

public final class AuthorizeHttpRequestsConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<AuthorizeHttpRequestsConfigurer<B>, B> {

    private final Registry      registry      = new Registry();
    private       RoleHierarchy roleHierarchy = RoleHierarchy.none();
    private       String        rolePrefix    = "ROLE_";

    public RoleHierarchy getRoleHierarchy() {
        return roleHierarchy;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public Registry getRegistry() {
        return registry;
    }

    public AuthorizeHttpRequestsConfigurer<B> roleHierarchy(RoleHierarchy hierarchy) {
        this.roleHierarchy = (hierarchy != null) ? hierarchy : RoleHierarchy.none();
        return this;
    }

    public AuthorizeHttpRequestsConfigurer<B> rolePrefix(String prefix) {
        this.rolePrefix = (prefix != null) ? prefix : "ROLE_";
        return this;
    }

    public void addMapping(
            List<RequestMatcher> requestMatchers, AuthorizationManager<RequestSecurityContext> manager, boolean negate) {
        registry.addMapping(requestMatchers, manager, negate);
    }

    @Override
    public void initialize(B builder) throws Exception {
        if (builder instanceof AbstractConfiguredSecurityBuilder<?, ?> securityBuilder) {
            securityBuilder.getBeanContext();
        }
    }

    @Override
    public void configure(B http) {
        http.addFilter(new OrderedFilter(new AuthorizationFilter(
                registry.createAuthorizationManager()
        ), 200));
    }

    private String getContextVariable(RequestSecurityContext context, String name) {
        return String.valueOf(
                context.match().variables().get(name)
        );
    }

    private AuthorizeHttpRequestsConfigurer<B>.Registry applyAuthorizationManager(
            AuthorizeHttpRequestsConfigurer<B>.Registry owner,
            List<RequestMatcher> matchers,
            AuthorizationManager<RequestSecurityContext> manager,
            boolean negate) {
        AuthorizationManager<RequestSecurityContext> effective = negate
                ? AuthorityPolicyAuthorizationManager.not(manager) : manager;
        addMapping(matchers, effective, negate);
        return owner;
    }

    final public class Registry extends AbstractRequestMatcherRegistry<AuthorizationCriterion<AuthorizeHttpRequestsConfigurer<B>.Registry, RequestSecurityContext>> {

        private final Builder              builder = new Builder();
        private       List<RequestMatcher> pending;

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

        public void addMapping(
                List<RequestMatcher> requestMatchers, AuthorizationManager<RequestSecurityContext> manager, boolean negate) {
            AuthorizationManager<RequestSecurityContext> effective = negate
                    ? AuthorityPolicyAuthorizationManager.not(manager) : manager;

            for (RequestMatcher requestMatcher : requestMatchers) {
                builder.addMapping(requestMatcher, effective);
            }

            this.pending = null;
        }

        @Override
        protected AuthorizationCriterion<AuthorizeHttpRequestsConfigurer<B>.Registry, RequestSecurityContext> applyMatchers(
                List<RequestMatcher> requestMatchers) {
            validatePending();
            this.pending = requestMatchers;
            AuthorizeHttpRequestsConfigurer<B> outer = AuthorizeHttpRequestsConfigurer.this;
            return new AuthorizationCriterion<>(
                    outer::applyAuthorizationManager,
                    outer.getRegistry(),
                    requestMatchers,
                    outer::getContextVariable
            );
        }

    }

}