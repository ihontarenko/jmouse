package org.jmouse.security.web.access;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.RequestMatcherEntry;
import org.jmouse.security.web.RequestMatch;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.ArrayList;
import java.util.List;

public class CompositeAuthorizationManager implements AuthorizationManager<HttpServletRequest> {

    private final List<RequestMatcherEntry<AuthorizationManager<RequestMatch>>> mappings;
    private final AuthorizationManager<HttpServletRequest>                      defaultManager;

    private CompositeAuthorizationManager(
            List<RequestMatcherEntry<AuthorizationManager<RequestMatch>>> entries,
            AuthorizationManager<HttpServletRequest> defaultManager
    ) {
        this.mappings = List.copyOf(entries);
        this.defaultManager = defaultManager;
    }

    @Override
    public AccessResult check(Authentication authentication, HttpServletRequest request) {
        RequestRoute requestRoute = RequestRoute.ofRequest(request);

        for (var mapping : mappings) {
            MappingMatcher<RequestRoute> matcher = mapping.matcher();
            if (matcher.matches(requestRoute)) {
                RequestMatch context = new RequestMatch(request, matcher.match(requestRoute));
                return mapping.entry().check(authentication, context);
            }
        }

        if (defaultManager != null) {
            return defaultManager.check(authentication, request);
        }

        return AccessResult.DENY;
    }

    public static final class Builder {

        private final List<RequestMatcherEntry<AuthorizationManager<RequestMatch>>> entries
                = new ArrayList<>();
        private       AuthorizationManager<HttpServletRequest>                      defaultManager;

        public Builder addMapping(MappingMatcher<RequestRoute> matcher, AuthorizationManager<RequestMatch> manager) {
            entries.add(new RequestMatcherEntry<>(matcher, manager));
            return this;
        }

        public Builder defaultManager(AuthorizationManager<HttpServletRequest> defaultManager) {
            this.defaultManager = defaultManager;
            return this;
        }

        public CompositeAuthorizationManager build() {
            if (!isEmpty()) {
                return new CompositeAuthorizationManager(entries, defaultManager);
            }

            throw new IllegalStateException("NO AUTHORIZATION MAPPINGS CONFIGURED!");
        }

        public boolean isEmpty() {
            return entries.isEmpty() && defaultManager == null;
        }
    }

}
