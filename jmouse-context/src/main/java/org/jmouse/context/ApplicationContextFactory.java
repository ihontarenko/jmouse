package org.jmouse.context;

/**
 * 🏭 Factory for creating {@link ApplicationBeanContext} instances.
 *
 * <p>Supports standalone and hierarchical context creation.</p>
 *
 * @param <T> type of the application context
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ApplicationContextFactory<T extends ApplicationBeanContext> {

    /**
     * 🎯 Creates a new application context with given ID and components.
     *
     * @param contextId unique context identifier
     * @param classes   component classes to register
     * @return new application context
     */
    T createContext(String contextId, Class<?>... classes);

    /**
     * 🧬 Creates a new context with a parent context.
     * <p>Allows dependency inheritance from the root context.</p>
     *
     * @param contextId   unique context identifier
     * @param rootContext parent context for inheritance
     * @param classes     component classes to register
     * @return new child application context
     */
    T createContext(String contextId, T rootContext, Class<?>... classes);

    /**
     * 🌱 Creates an empty root-level application context.
     *
     * @return root application context
     */
    T createRootContext();

    /**
     * 🌱 Creates an empty application-level context.
     *
     * @return application context
     */
    T createApplicationContext(String contextId, T rootContext, Class<?>... classes);
}
