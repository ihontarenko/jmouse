package org.jmouse.security.web;

import jakarta.servlet.http.HttpServletRequest;

public record EnrichedRequestContext(HttpServletRequest request, MatchResult match) {
}
