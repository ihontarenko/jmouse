package org.jmouse.security.web.access;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.MatchResult;
import org.jmouse.security.web.RequestSecurityContext;
import org.jmouse.security.web.match.RequestMatcherEntry;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.ArrayList;
import java.util.List;

public class DelegatingAuthorizationManager implements AuthorizationManager<HttpServletRequest> {

    private final List<RequestMatcherEntry<AuthorizationManager<RequestSecurityContext>>> mappings;
    private final AuthorizationManager<HttpServletRequest>                                defaultManager;

    private DelegatingAuthorizationManager(
            List<RequestMatcherEntry<AuthorizationManager<RequestSecurityContext>>> entries,
            AuthorizationManager<HttpServletRequest> defaultManager
    ) {
        this.mappings = List.copyOf(entries);
        this.defaultManager = defaultManager;
    }

    @Override
    public AccessResult check(Authentication authentication, HttpServletRequest request) {

        RequestRoute requestRoute = RequestRoute.ofRequest(request);

//        MappingCriteria mappingCriteria = new MappingCriteria(
//                Route.GET("/aaa")
//        );

        for (var mapping : mappings) {
            MappingMatcher matcher = mapping.matcher();
//            MatchResult    match   = matcher.match(request);
            if (matcher.matches(requestRoute)) {
                AuthorizationManager<RequestSecurityContext> authorizationManager = mapping.entry();
//                RequestSecurityContext                       context              = new RequestSecurityContext(request, match);
//                return authorizationManager.check(authentication, context);


            }
        }

        if (defaultManager != null) {
            return defaultManager.check(authentication, request);
        }

        return AccessResult.DENY;
    }

    public static final class Builder {

        private final List<RequestMatcherEntry<AuthorizationManager<RequestSecurityContext>>> entries
                = new ArrayList<>();
        private       AuthorizationManager<HttpServletRequest>                                defaultManager;

        public Builder addMapping(MappingMatcher matcher, AuthorizationManager<RequestSecurityContext> manager) {
            entries.add(new RequestMatcherEntry<>(matcher, manager));
            return this;
        }

        public Builder defaultManager(AuthorizationManager<HttpServletRequest> defaultManager) {
            this.defaultManager = defaultManager;
            return this;
        }

        public DelegatingAuthorizationManager build() {
            if (!isEmpty()) {
                return new DelegatingAuthorizationManager(entries, defaultManager);
            }

            throw new IllegalStateException("NO AUTHORIZATION MAPPINGS CONFIGURED!");
        }

        public boolean isEmpty() {
            return entries.isEmpty() && defaultManager == null;
        }
    }

}
