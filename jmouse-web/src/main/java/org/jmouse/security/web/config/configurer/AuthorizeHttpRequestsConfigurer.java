package org.jmouse.security.web.config.configurer;

import org.jmouse.security.authorization.AuthorityPolicyAuthorizationManager;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.access.RoleHierarchy;
import org.jmouse.security.web.AuthorizationFilter;
import org.jmouse.security.web.RequestSecurityContext;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.config.AbstractRequestMatcherRegistry;
import org.jmouse.security.web.config.AuthorizationCriterion;
import org.jmouse.security.web.access.DelegatingAuthorizationManager.Builder;
import org.jmouse.security.web.config.HttpSecurityBuilder;
import org.jmouse.security.web.config.SecurityConfigurer;

import java.util.List;

public final class AuthorizeHttpRequestsConfigurer<B extends HttpSecurityBuilder<B>> implements SecurityConfigurer<B> {

    private final Registry registry = new Registry();

    private final Builder       mappings      = new Builder();
    private       RoleHierarchy roleHierarchy = RoleHierarchy.none();
    private       String        rolePrefix    = "ROLE_";

    private List<RequestMatcher> requestMatchers;

    public RoleHierarchy getRoleHierarchy() {
        return roleHierarchy;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public AuthorizeHttpRequestsConfigurer<B> roleHierarchy(RoleHierarchy hierarchy) {
        this.roleHierarchy = (hierarchy != null) ? hierarchy : RoleHierarchy.none();
        return this;
    }

    public AuthorizeHttpRequestsConfigurer<B> rolePrefix(String prefix) {
        this.rolePrefix = (prefix != null) ? prefix : "ROLE_";
        return this;
    }

    public AuthorizationCriterion<AuthorizeHttpRequestsConfigurer<B>, RequestSecurityContext> requestMatchers(RequestMatcher... matchers) {
        requestMatchers = List.of(matchers);
        return new AuthorizationCriterion<>(
                this::applyAuthorizationManager, this, requestMatchers, this::getContextVariable);
    }

    public AuthorizationCriterion<AuthorizeHttpRequestsConfigurer<B>, RequestSecurityContext> anyRequest() {
        return requestMatchers(RequestMatcher.any());
    }

    public void addMapping(
            List<RequestMatcher> requestMatchers, AuthorizationManager<RequestSecurityContext> manager, boolean negate) {
        for (RequestMatcher requestMatcher : requestMatchers) {
            registry.addMapping();
        }
    }

    @Override
    public void configure(B builder) {
        builder.addFilter(new OrderedFilter(new AuthorizationFilter(mappings.build()), 200));
    }

    private String getContextVariable(RequestSecurityContext context, String name) {
        return String.valueOf(
                context.match().variables().get(name)
        );
    }

    private AuthorizeHttpRequestsConfigurer<B> applyAuthorizationManager(
            AuthorizeHttpRequestsConfigurer<B> owner,
            List<RequestMatcher> matchers,
            AuthorizationManager<RequestSecurityContext> manager,
            boolean negate
    ) {
        var effective = negate ? AuthorityPolicyAuthorizationManager.not(manager) : manager;

        for (RequestMatcher requestMatcher : matchers) {
            mappings.addMapping(requestMatcher, effective);
        }

        return owner;
    }

    final class Registry extends AbstractRequestMatcherRegistry<AuthorizationCriterion<AuthorizeHttpRequestsConfigurer<B>, RequestSecurityContext>> {

        private final Builder builder = new Builder();
        private List<RequestMatcher> pending;

        private void ensureNoPending() {

        }

        public void addMapping(
                List<RequestMatcher> requestMatchers, AuthorizationManager<RequestSecurityContext> manager, boolean negate) {
            AuthorizeHttpRequestsConfigurer<B>           outer     = AuthorizeHttpRequestsConfigurer.this;
            AuthorizationManager<RequestSecurityContext> effective = negate
                    ? AuthorityPolicyAuthorizationManager.not(manager) : manager;

            for (RequestMatcher requestMatcher : requestMatchers) {
                builder.addMapping(requestMatcher, effective);
            }
        }

        @Override
        protected AuthorizationCriterion<AuthorizeHttpRequestsConfigurer<B>, RequestSecurityContext> applyMatchers(
                List<RequestMatcher> requestMatchers) {
            AuthorizeHttpRequestsConfigurer<B> outer = AuthorizeHttpRequestsConfigurer.this;
            return new AuthorizationCriterion<>(
                    outer::applyAuthorizationManager, outer, requestMatchers, outer::getContextVariable);
        }

    }

}