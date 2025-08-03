package org.jmouse.mvc;

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
 * @param match  the result of route pattern matching
 * @param route       the declared route definition
 *
 * @see Route
 * @see RouteMatch
 * @see MappedHandler
 *
 * ✍️ Author: Ivan Hontarenko (Mr. Jerry Mouse)
 * 📧 Email: ihontarenko@gmail.com
 */
public record RouteMappedHandler(Object handler, RouteMatch match, Route route) implements MappedHandler {
}
