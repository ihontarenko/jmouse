package org.jmouse.context;

import org.jmouse.core.env.Environment;

/**
 * A factory interface for creating instances of {@link ApplicationBeanContext}.
 * <p>
 * This interface extends {@link ApplicationContextFactory} and provides an additional method
 * for creating a default {@link Environment} that can be used within application contexts.
 * </p>
 *
 * @param <T> the type of {@link ApplicationBeanContext} that this factory creates
 */
public interface ApplicationFactory<T extends ApplicationBeanContext> extends ApplicationContextFactory<T> {

    /**
     * Creates a new default {@link Environment} instance.
     * <p>
     * The created environment can be used to manage configuration properties,
     * system settings, and profiles within the application context.
     * </p>
     *
     * @return a new {@link Environment} instance with default settings
     */
    Environment createDefaultEnvironment();

}
