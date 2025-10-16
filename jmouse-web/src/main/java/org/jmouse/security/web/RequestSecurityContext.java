package org.jmouse.security.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.matcher.Match;

public record RequestSecurityContext(HttpServletRequest request, Match match) {
}
