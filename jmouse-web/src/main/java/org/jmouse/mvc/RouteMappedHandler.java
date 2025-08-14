package org.jmouse.mvc;

import org.jmouse.core.MethodParameter;

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
 * @param handler     the actual handler to be executed
 * @param mappingResult  the result of route pattern matching
 *
 * @see Route
 * @see RouteMatch
 * @see MappedHandler
 *
 * ✍️ Author: Ivan Hontarenko (Mr. Jerry Mouse)
 * 📧 Email: ihontarenko@gmail.com
 */
public record RouteMappedHandler(Object handler, MappingResult mappingResult, MethodParameter methodParameter)
        implements MappedHandler {
}
