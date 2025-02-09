package org.jmouse.context;

import org.jmouse.beans.BeanContext;

/**
 * A factory interface for creating application contexts.
 * <p>
 * This interface provides methods to initialize an {@link ApplicationBeanContext} instance,
 * either as a standalone context or as a child of an existing {@link BeanContext}.
 * </p>
 *
 * @param <T> the type of the {@link ApplicationBeanContext} that this factory creates
 */
public interface ApplicationContextFactory<T extends ApplicationBeanContext> {

    /**
     * Creates a new application context with the specified context ID.
     * <p>
     * The created context is initialized with the given component classes.
     * </p>
     *
     * @param contextId the unique identifier for the application context
     * @param classes   the component classes to be registered in the context
     * @return a new instance of {@code T} representing the application context
     */
    T createContext(String contextId, Class<?>... classes);

    /**
     * Creates a new application context with the specified context ID and parent context.
     * <p>
     * This method allows creating a hierarchical context structure, where the newly created
     * context inherits dependencies from the given {@code rootContext}.
     * </p>
     *
     * @param contextId   the unique identifier for the application context
     * @param rootContext the parent {@link BeanContext} from which dependencies can be inherited
     * @param classes     the component classes to be registered in the new context
     * @return a new instance of {@code T} representing the child application context
     */
    T createContext(String contextId, T rootContext, Class<?>... classes);

}
