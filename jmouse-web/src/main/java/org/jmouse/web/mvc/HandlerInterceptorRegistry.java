package org.jmouse.web.mvc;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 📦 Registry for storing {@link HandlerInterceptor} instances.
 * <p>
 * Interceptors are executed in the order they are registered.
 *
 * <pre>{@code
 * HandlerInterceptorRegistry registry = new HandlerInterceptorRegistry();
 * registry.addInterceptor(new LoggingInterceptor());
 * registry.addInterceptor(new AuthInterceptor());
 * List<HandlerInterceptor> list = registry.getInterceptors();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class HandlerInterceptorRegistry {

    private final List<HandlerInterceptor> interceptors = new LinkedList<>();

    /**
     * Adds a new {@link HandlerInterceptor} to the registry.
     *
     * @param interceptor the interceptor to add
     * @return this registry for chaining
     */
    public HandlerInterceptorRegistry addInterceptor(HandlerInterceptor interceptor) {
        if (interceptor != null) {
            this.interceptors.add(interceptor);
        }
        return this;
    }

    /**
     * Returns all registered {@link HandlerInterceptor}s in registration order.
     *
     * @return an unmodifiable list of interceptors
     */
    public List<HandlerInterceptor> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }

    /**
     * Clears all registered interceptors.
     */
    public void clear() {
        interceptors.clear();
    }

    /**
     * Returns whether any interceptors have been registered.
     *
     * @return true if the registry is empty
     */
    public boolean isEmpty() {
        return interceptors.isEmpty();
    }

    /**
     * Returns the number of registered interceptors.
     *
     * @return the count of interceptors
     */
    public int size() {
        return interceptors.size();
    }
}
