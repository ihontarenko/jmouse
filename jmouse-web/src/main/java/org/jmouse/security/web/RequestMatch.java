package org.jmouse.security.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.matcher.Match;

/**
 * 📦 Represents a successful mapping between an incoming {@link HttpServletRequest}
 * and a resolved {@link Match} context.
 * <p>
 * Used to carry both the original HTTP request and its corresponding
 * route or security match result through the filter/authorization chain.
 * </p>
 *
 * @param request the incoming HTTP request
 * @param match   the evaluated match result (path, headers, attributes, etc.)
 */
public record RequestMatch(HttpServletRequest request, Match match) { }
