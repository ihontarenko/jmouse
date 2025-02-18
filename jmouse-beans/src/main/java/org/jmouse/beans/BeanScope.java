package org.jmouse.beans;

/**
 * Enum representing the lifecycle of a structured in the context.
 * Defines various scopes for managing structured instances.
 */
public enum BeanScope implements Scope {

    /**
     * A new instance of the structured is created every time it is requested.
     * <p>
     * This is suitable for lightweight and stateless beans where
     * multiple instances are acceptable.
     */
    PROTOTYPE,

    /**
     * A single instance of the structured is created and shared across the context.
     * <p>
     * This is the default scope and is suitable for most use cases
     * where a shared instance is required.
     */
    SINGLETON,

    /**
     * A new instance of the structured is created for each HTTP request.
     * <p>
     * This scope is applicable only in web contexts and is useful
     * for handling request-specific data.
     */
    REQUEST,

    /**
     * A single instance of the structured is created per HTTP session.
     * <p>
     * This scope is applicable only in web contexts and is useful
     * for managing session-specific data, such as user preferences
     * or session-level cache.
     */
    SESSION,

    /**
     * Indicates that the class is not a structured and should not be managed
     * by the container.
     * <p>
     * This is typically used for marker interfaces or classes that
     * are part of the internal container logic.
     */
    NON_BEAN;


    /**
     * Returns the unique identifier for this scope, which is the ordinal
     * value of the enum constant.
     *
     * @return the unique identifier for the scope.
     */
    @Override
    public int id() {
        return ordinal();
    }
}
