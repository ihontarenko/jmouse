package org.jmouse.mvc.routing;

/**
 * Represents a routable mapping unit (path, conditions, handler).
 */
public interface Mapping<T> {

    /**
     * @return Route conditions (path, method, media types, etc).
     */
    MappingCondition getCondition();

    /**
     * @return The target handler (method, function, controller, etc).
     */
    T getHandler();
}