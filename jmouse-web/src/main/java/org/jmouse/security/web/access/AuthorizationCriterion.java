package org.jmouse.security.web.access;

import org.jmouse.security.web.RequestMatcher;

import java.util.List;

public class AuthorizationCriterion {

    private final List<RequestMatcher> matchers;

    public AuthorizationCriterion(List<RequestMatcher> matchers) {
        this.matchers = matchers;
    }



}
