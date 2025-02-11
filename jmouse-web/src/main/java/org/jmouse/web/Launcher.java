package org.jmouse.web;

import org.jmouse.beans.BeanContext;

/**
 * Defines a contract for launching an application with a configurable {@link BeanContext}.
 *
 * @param <C> the type of {@link BeanContext} used during application launch
 */
public interface Launcher<C extends BeanContext> {

    /**
     * Launches the application and returns the initialized {@link BeanContext}.
     *
     * @param arguments optional command-line arguments
     * @return the fully initialized {@link BeanContext} instance
     */
    C launch(String... arguments);
}
