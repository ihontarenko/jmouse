package org.jmouse.security.web;

import jakarta.servlet.http.HttpServletRequest;

public record RequestSecurityContext(HttpServletRequest request, MatchResult match) {
}
