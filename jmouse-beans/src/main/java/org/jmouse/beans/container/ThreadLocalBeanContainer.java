package org.jmouse.beans.container;

import org.jmouse.beans.BeanContainer;
import org.jmouse.beans.Scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link BeanContainer} implementation that manages beans in a thread-local scope.
 * <p>
 * Beans registered in this container are stored separately for each thread, ensuring thread-safe
 * management of thread-specific beans.
 */
public class ThreadLocalBeanContainer implements BeanContainer {

    /**
     * A thread-local map for storing beans, where each thread maintains its own independent map of beans.
     */
    private final ThreadLocal<Map<String, Object>> threadLocalBeans = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * Retrieves a structured instance by its name from the thread-local context.
     *
     * @param name the name of the structured to retrieve.
     * @param <T>  the type of the structured.
     * @return the structured instance if present, or {@code null} if no structured is found with the given name.
     */
    @Override
    public <T> T getBean(String name) {
        return (T) threadLocalBeans.get().get(name);
    }

    /**
     * Registers a structured instance with the given name in the thread-local context.
     *
     * @param name the name of the structured.
     * @param bean the structured instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {
        threadLocalBeans.get().put(name, bean);
    }

    /**
     * Checks if a structured with the specified name exists in the thread-local context.
     *
     * @param name the name of the structured.
     * @return {@code true} if a structured with the given name exists, {@code false} otherwise.
     */
    @Override
    public boolean containsBean(String name) {
        return threadLocalBeans.get().containsKey(name);
    }

    /**
     * Determines if this container supports the given {@link Scope}.
     *
     * @param scope the {@link Scope} to check.
     * @return {@code true} if the scope matches the thread-local scope, {@code false} otherwise.
     */
    @Override
    public boolean supports(Scope scope) {
        return ThreadLocalScope.THREAD_LOCAL_SCOPE.equals(scope);
    }
}
