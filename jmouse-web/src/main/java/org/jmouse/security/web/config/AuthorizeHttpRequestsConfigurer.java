package org.jmouse.security.web.config;

import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.access.RoleHierarchy;
import org.jmouse.security.web.AuthorizationFilter;
import org.jmouse.security.web.EnrichedRequestContext;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.access.AuthorizationCriterion;
import org.jmouse.security.web.access.AuthorizationPolicies;
import org.jmouse.security.web.access.DelegatingAuthorizationManager.Builder;

import java.util.List;

public final class AuthorizeHttpRequestsConfigurer<B extends HttpSecurityBuilder<B>> implements SecurityConfigurer<B> {

    private final Builder       mappings      = new Builder();
    private RoleHierarchy roleHierarchy = RoleHierarchy.none();
    private String        rolePrefix    = "ROLE_";

    private List<RequestMatcher> pendingMatchers;

    public AuthorizationCriterion<AuthorizeHttpRequestsConfigurer<B>> requestMatchers(RequestMatcher... matchers) {
        pendingMatchers = List.of(matchers);
        return new AuthorizationCriterion<>(this::applyMapping, this, pendingMatchers);
    }

    public AuthorizationCriterion<AuthorizeHttpRequestsConfigurer<B>> anyRequest() {
        return requestMatchers(RequestMatcher.any());
    }

    public void addMapping(List<RequestMatcher> requestMatchers, AuthorizationManager<EnrichedRequestContext> manager, boolean negate) {
        AuthorizationManager<EnrichedRequestContext> effective = negate ? AuthorizationPolicies.not(manager) : manager;

        for (RequestMatcher requestMatcher : requestMatchers) {
            mappings.addMapping(requestMatcher, effective);
        }

        this.pendingMatchers = null;
    }

    @Override
    public void configure(B builder) {
        builder.addFilter(new OrderedFilter(new AuthorizationFilter(mappings.build()), 200));
    }

    private AuthorizeHttpRequestsConfigurer<B> applyMapping(AuthorizeHttpRequestsConfigurer<B> owner, List<RequestMatcher> matchers, AuthorizationManager<EnrichedRequestContext> manager, boolean negate) {
        var effective = negate ? AuthorizationPolicies.not(manager) : manager;

        for (RequestMatcher requestMatcher : matchers) {
            mappings.addMapping(requestMatcher, effective);
        }

        return owner;
    }
}