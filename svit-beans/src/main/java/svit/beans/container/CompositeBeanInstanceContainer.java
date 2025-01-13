package svit.beans.container;

import svit.beans.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompositeBeanInstanceContainer implements DelegatingBeanContainer {

    /**
     * Retrieves a bean instance by its name.
     *
     * @param name the name of the bean to retrieve.
     * @return the bean instance, or {@code null} if no bean is found with the given name.
     */
    @Override
    public <T> T getBean(String name) {
        return null;
    }

    /**
     * Registers a bean instance with the given name.
     *
     * @param name the name of the bean.
     * @param bean the bean instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {

    }

    /**
     * Retrieves the {@link BeanInstanceContainer} associated with the specified {@link Scope}.
     *
     * @param scope the {@link Scope} for which to retrieve the container.
     * @return the {@link BeanInstanceContainer} associated with the provided scope,
     * or {@code null} if no container is registered for the scope.
     */
    @Override
    public BeanInstanceContainer getBeanInstanceContainer(Scope scope) {
        return null;
    }

    /**
     * Registers a {@link BeanInstanceContainer} for a specific {@link Scope}.
     * <p>
     * Associates the given container with the provided scope, enabling
     * management of beans within that scope.
     * </p>
     *
     * @param scope     the {@link Scope} for which the container is being registered.
     * @param container the {@link BeanInstanceContainer} to associate with the scope.
     */
    @Override
    public void registerBeanInstanceContainer(Scope scope, BeanInstanceContainer container) {

    }

    /**
     * Removes all registered {@link BeanInstanceContainer}s.
     * <p>
     * Clears all previously registered containers, effectively resetting the registry.
     * </p>
     */
    @Override
    public void removeBeanInstanceContainers() {

    }

    /**
     * Determines if the current implementation supports the given {@link Scope}.
     * <p>
     * By default, this method returns {@code false}, indicating that the scope
     * is not supported. Implementations can override this method to specify
     * which scopes they support.
     *
     * @param scope the {@link Scope} to check for support.
     * @return {@code true} if the scope is supported, {@code false} otherwise.
     */
    @Override
    public boolean supports(Scope scope) {
        return DelegatingBeanContainer.super.supports(scope);
    }

    /**
     * Checks whether a bean with the specified name is already registered in this container.
     *
     * @param name the name of the bean.
     * @return {@code true} if a bean with the given name exists, otherwise {@code false}.
     */
    @Override
    public boolean containsBean(String name) {
        return DelegatingBeanContainer.super.containsBean(name);
    }
}
