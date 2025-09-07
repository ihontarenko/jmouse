package org.jmouse.web;

import org.jmouse.beans.BeanContext;

/**
 * 🚀 Base contract for launching an application with a configurable {@link BeanContext}.
 *
 * @param <C> the type of {@link BeanContext} used during application launch
 */
public interface Launcher<C extends BeanContext> {

    /**
     * ▶️ Launch the application and return the initialized context.
     *
     * @param arguments optional command-line arguments
     * @return fully initialized {@link BeanContext}
     */
    C launch(String... arguments);

    /**
     * 🔥 Warm up the application context before it is used.
     *
     * <p>Intended for eager initialization of beans, caches,
     * or other resources that should be prepared early.</p>
     *
     * @param context current bean context
     */
    void warmup(C context);
}
