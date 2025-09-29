package org.jmouse.security.web.access;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.authorization.AccessResult;
import org.jmouse.security.authorization.AuthorizationManager;
import org.jmouse.security.core.Authentication;
import org.jmouse.security.web.RequestAuthorizationContext;
import org.jmouse.security.web.match.RequestMatcherEntry;

import java.util.LinkedList;
import java.util.List;

public class DelegatingAuthorizationManager implements AuthorizationManager<HttpServletRequest> {

    private final List<RequestMatcherEntry<AuthorizationManager<RequestAuthorizationContext>>> mapping;

    public DelegatingAuthorizationManager() {
        this.mapping = new LinkedList<>();
    }

    @Override
    public AccessResult check(Authentication authentication, HttpServletRequest target) {
        return null;
    }

}
