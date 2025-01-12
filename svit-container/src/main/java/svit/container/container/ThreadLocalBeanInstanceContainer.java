package svit.container.container;

import svit.container.BeanInstanceContainer;
import svit.container.Scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link BeanInstanceContainer} implementation that manages beans in a thread-local scope.
 * <p>
 * Beans registered in this container are stored separately for each thread, ensuring thread-safe
 * management of thread-specific beans.
 */
public class ThreadLocalBeanInstanceContainer implements BeanInstanceContainer {

    /**
     * A thread-local map for storing beans, where each thread maintains its own independent map of beans.
     */
    private final ThreadLocal<Map<String, Object>> threadLocalBeans = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * Retrieves a bean instance by its name from the thread-local context.
     *
     * @param name the name of the bean to retrieve.
     * @param <T>  the type of the bean.
     * @return the bean instance if present, or {@code null} if no bean is found with the given name.
     */
    @Override
    public <T> T getBean(String name) {
        return (T) threadLocalBeans.get().get(name);
    }

    /**
     * Registers a bean instance with the given name in the thread-local context.
     *
     * @param name the name of the bean.
     * @param bean the bean instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {
        threadLocalBeans.get().put(name, bean);
    }

    /**
     * Checks if a bean with the specified name exists in the thread-local context.
     *
     * @param name the name of the bean.
     * @return {@code true} if a bean with the given name exists, {@code false} otherwise.
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
