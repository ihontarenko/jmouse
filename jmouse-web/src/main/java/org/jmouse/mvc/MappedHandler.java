package org.jmouse.mvc;

import org.jmouse.core.MethodParameter;

/**
 * 🔍 Represents a resolved handler along with its route mapping metadata.
 *
 * <p>This interface is used internally by {@link HandlerMapping} implementations
 * to encapsulate the handler instance (e.g., a controller method or lambda)
 * and associated routing information.</p>
 *
 * <p>Handlers may be mapped explicitly to routes, in which case
 * {@link #mappingResult()}, {@link #route()} and {@link #match()} provide
 * metadata about the route and the route matching result.</p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * if (resolution.isMapped()) {
 *     Route route = resolution.route();
 *     Object handler = resolution.handler();
 *     // invoke handler using the resolved route and handler instance
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface MappedHandler {

    /**
     * 🎯 Returns the actual handler object to invoke, such as a method, lambda, or controller instance.
     *
     * @return the resolved handler instance
     */
    Object handler();

    /**
     * Returns the method parameter metadata if applicable.
     * <p>By default, returns {@code null}.</p>
     *
     * @return method parameter information or {@code null} if not applicable
     */
    default MethodParameter methodParameter() {
        return null;
    }

    /**
     * Returns the {@link MappingResult} containing route and match metadata.
     * <p>By default, returns {@code null} if this handler is not route-mapped.</p>
     *
     * @return mapping result or {@code null} if unmapped
     */
    default MappingResult mappingResult() {
        return null;
    }

    /**
     * 🧭 Returns the route definition if this handler is mapped via an explicit route.
     *
     * @return the {@link Route} or {@code null} if not mapped
     */
    default Route route() {
        return isMapped() ? mappingResult().route() : null;
    }

    /**
     * 🧭 Returns the route match details if this handler is mapped via an explicit route.
     *
     * @return the {@link RouteMatch} or {@code null} if not mapped
     */
    default RouteMatch match() {
        return isMapped() ? mappingResult().match() : null;
    }

    /**
     * ❓ Indicates whether this handler is associated with a mapped route.
     *
     * @return {@code true} if a route mapping exists, {@code false} otherwise
     */
    default boolean isMapped() {
        return mappingResult() != null;
    }
}
