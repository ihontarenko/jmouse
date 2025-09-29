package org.jmouse.security.web;

import jakarta.servlet.http.HttpServletRequest;

public record RequestAuthorizationContext(HttpServletRequest request, MatchResult match) {
}
