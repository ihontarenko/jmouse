package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 🌐 Immutable wrapper for the current HTTP request and response.
 *
 * <p>Used to pass around servlet context in a unified way between components
 * such as resolvers, interceptors, and handler adapters.
 *
 * @param request  the current HTTP servlet request
 * @param response the current HTTP servlet response
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record RequestContext(HttpServletRequest request, HttpServletResponse response) { }
