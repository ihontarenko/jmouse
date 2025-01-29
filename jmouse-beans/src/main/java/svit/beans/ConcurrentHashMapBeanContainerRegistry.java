package svit.beans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A simple implementation of the {@link BeanContainerRegistry} interface.
 * <p>
 * This class provides a mechanism to register, retrieve, and manage {@link BeanContainer}s
 * for different {@link Scope}s. It uses a thread-safe {@link ConcurrentHashMap} to
 * dynamically associate scopes with their corresponding containers.
 * </p>
 *
 * @see BeanContainer
 * @see Scope
 * @see BeanScope
 */
public class ConcurrentHashMapBeanContainerRegistry implements BeanContainerRegistry {

    /**
     * A mapping of {@link Scope} to their respective {@link BeanContainer}.
     * <p>
     * This map is used to dynamically associate scopes with their corresponding
     * containers, allowing for flexible management of bean instances based on scope.
     * </p>
     */
    private final Map<Scope, BeanContainer> containers = new ConcurrentHashMap<>();

    /**
     * Retrieves the {@link BeanContainer} associated with the specified {@link Scope}.
     *
     * @param scope the {@link Scope} for which to retrieve the container.
     * @return the {@link BeanContainer} associated with the provided scope,
     * or {@code null} if no container is registered for the scope.
     * @throws BeanContextException if no container is registered for the provided scope.
     */
    @Override
    public BeanContainer getBeanContainer(Scope scope) {
        BeanContainer instanceContainer = containers.get(scope);

        if (instanceContainer == null) {
            throw new UnsupportedScopeException(scope, getClass());
        }

        return instanceContainer;
    }

    /**
     * Registers a {@link BeanContainer} for a specific {@link Scope}.
     * <p>
     * Associates the given container with the provided scope, enabling
     * management of beans within that scope.
     * </p>
     *
     * @param scope     the {@link Scope} for which the container is being registered.
     * @param container the {@link BeanContainer} to associate with the scope.
     * @throws IllegalArgumentException if the scope or container is {@code null}.
     */
    @Override
    public void registerBeanContainer(Scope scope, BeanContainer container) {
        containers.put(scope, container);
    }

    /**
     * Removes all registered {@link BeanContainer}s.
     * <p>
     * Clears all previously registered containers, effectively resetting the registry.
     * </p>
     */
    @Override
    public void removeBeanInstanceContainers() {
        containers.clear();
    }

    /**
     * Checks if a {@link BeanContainer} is registered for the specified {@link Scope}.
     *
     * @param scope the scope to check for a registered bean container.
     * @return {@code true} if a container is registered for the given scope, {@code false} otherwise.
     * @throws IllegalArgumentException if the scope is {@code null}.
     */
    @Override
    public boolean containsBeanContainer(Scope scope) {
        return containers.containsKey(scope);
    }

    @Override
    public String toString() {
        return "BeanContainers: %s".formatted(containers.keySet());
    }

}
