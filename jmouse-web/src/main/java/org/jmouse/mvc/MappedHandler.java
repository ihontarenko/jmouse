package org.jmouse.mvc;

/**
 * 🔍 Resolves the handler and its route mapping.
 * <p>
 * Used internally by {@code HandlerMapping} to wrap handler method or instance
 * along with optional {@link Route} metadata.
 * </p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * if (resolution.isMapped()) {
 *     Route route = resolution.route();
 *     Object handler = resolution.handler();
 *     // invoke handler
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface MappedHandler {

    /**
     * 🎯 The resolved handler (e.g. method, lambda, controller).
     *
     * @return the handler object
     */
    Object handler();

    /**
     * 🧭 Optional route definition if mapped via explicit route.
     *
     * @return associated {@link Route} or {@code null} if not mapped
     */
    default Route route() {
        return null;
    }

    /**
     * ❓ Indicates whether this resolution includes a mapped route.
     *
     * @return {@code true} if {@link #route()} is not {@code null}
     */
    default boolean isMapped() {
        return route() != null;
    }
}
