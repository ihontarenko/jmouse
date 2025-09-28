package org.jmouse.web.mvc;

import org.jmouse.core.MethodParameter;
import org.jmouse.web.match.Route;
import org.jmouse.web.match.RouteMatch;

/**
 * 🧭 A {@link MappedHandler} that encapsulates a route match, the associated route,
 * and the actual handler to invoke.
 *
 * <p>Used by routing components after a successful {@code RouteMatch} to pass
 * full contextual data to handler adapters.</p>
 *
 * <pre>{@code
 * RouteMappedHandler mapped = new RouteMappedHandler(controller, match, route);
 * Object actualHandler = mapped.handler();
 * }</pre>
 *
 * @see Route
 * @see RouteMatch
 * @see MappedHandler
 *
 * ✍️ Author: Ivan Hontarenko (Mr. Jerry Mouse)
 * 📧 Email: ihontarenko@gmail.com
 */
public record RouteMappedHandler(Object handler, MappingResult mappingResult, MethodParameter returnParameter)
        implements MappedHandler {
}
