package org.jmouse.web.http;

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
public record RequestContext(HttpServletRequest request, HttpServletResponse response) {

    /**
     * Returns a request-scoped attribute by name. 📦
     *
     * @param attribute attribute name
     *
     * @return attribute value or {@code null} if not present
     */
    public Object getRequestAttribute(String attribute) {
        return request.getAttribute(attribute);
    }

    /**
     * Returns a typed request-scoped attribute. 🎯
     *
     * <p>Performs a safe {@link Class#isInstance(Object)} check before casting.</p>
     *
     * <p>If {@code required} is {@code true} and the attribute is either missing
     * or not assignable to the requested type, an exception is thrown.</p>
     *
     * @param <T>       expected attribute type
     * @param attribute attribute name
     * @param type      expected type
     * @param required  whether the attribute must be present and assignable
     *
     * @return attribute value cast to {@code T}, or {@code null} if not required and unavailable
     *
     * @throws IllegalArgumentException if {@code required} is {@code true} and the attribute
     *                                  is missing or not of the expected type
     */
    public <T> T getRequestAttribute(String attribute, Class<T> type, boolean required) {
        Object object = getRequestAttribute(attribute);
        T      value  = null;

        if (type.isInstance(object)) {
            value = type.cast(object);
        }

        if (required && value == null) {
            String actualType = (object != null ? object.getClass().getName() : "null");
            throw new IllegalArgumentException(
                    "Required request attribute '%s' of type '%s' not found (actual: %s)"
                            .formatted(attribute, type.getName(), actualType)
            );
        }

        return value;
    }

    /**
     * Returns a typed request-scoped attribute. 📦
     *
     * <p>Shortcut for {@link #getRequestAttribute(String, Class, boolean)} with
     * {@code required = false}.</p>
     *
     * @param <T>       expected attribute type
     * @param attribute attribute name
     * @param type      expected type
     *
     * @return attribute value cast to {@code T}, or {@code null} if absent or not assignable
     */
    public <T> T getRequestAttribute(String attribute, Class<T> type) {
        return getRequestAttribute(attribute, type, false);
    }

}
