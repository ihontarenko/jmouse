package org.jmouse.beans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A global implementation of {@link BeanContainerRegistry} that dynamically resolves and delegates to scoped
 * {@link BeanContainerRegistry} instances.
 * <p>
 * The {@code GlobalsBeanContainerRegistry} allows for managing beans across multiple registries that are associated
 * with specific scopes, enabling tenant-specific or contextualized bean management.
 * </p>
 *
 * <p>Key Features:</p>
 * <ul>
 *     <li>Dynamically resolves a {@link BeanContainerRegistry} for a given {@link Scope}.</li>
 *     <li>Supports lazy initialization of registries using a provided {@link ObjectFactory}.</li>
 *     <li>Delegates bean registration, retrieval, and removal operations to the resolved registry.</li>
 * </ul>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * ObjectFactory<BeanContainerRegistry> factory = DefaultBeanContainerRegistry::new;
 * GlobalsBeanContainerRegistry globalRegistry = new GlobalsBeanContainerRegistry(factory);
 *
 * Scope tenantScope = new CustomScope("tenant1");
 * BeanContainer container = new DefaultBeanContainer();
 *
 * globalRegistry.registerBeanContainer(tenantScope, container);
 * }</pre>
 *
 * @see BeanContainerRegistry
 */
public class GlobalsBeanContainerRegistry implements BeanContainerRegistry {

    private final ObjectFactory<BeanContainerRegistry> defaultContainerRegistry;
    private final Map<String, BeanContainerRegistry>   containerRegistries = new ConcurrentHashMap<>();

    /**
     * Constructs a {@code GlobalsBeanContainerRegistry} with the specified factory for creating scoped registries.
     *
     * @param defaultContainerRegistry the {@link ObjectFactory} to use for creating new {@link BeanContainerRegistry} instances.
     */
    public GlobalsBeanContainerRegistry(ObjectFactory<BeanContainerRegistry> defaultContainerRegistry) {
        this.defaultContainerRegistry = defaultContainerRegistry;
    }

    /**
     * Retrieves the {@link BeanContainer} associated with the specified {@link Scope}.
     * <p>
     * Currently returns {@code null}, and this behavior should be overridden in a subclass or updated
     * to provide meaningful implementation.
     * </p>
     *
     * @param scope the scope for which to retrieve the {@link BeanContainer}.
     * @return the {@link BeanContainer} associated with the provided scope, or {@code null}.
     */
    @Override
    public BeanContainer getBeanContainer(Scope scope) {
        return resolveContainerRegistry().getBeanContainer(scope);
    }

    /**
     * Registers a {@link BeanContainer} for the specified {@link Scope}.
     *
     * @param scope     the scope for which the container is being registered.
     * @param container the {@link BeanContainer} to associate with the scope.
     */
    @Override
    public void registerBeanContainer(Scope scope, BeanContainer container) {
        resolveContainerRegistry().registerBeanContainer(scope, container);
    }

    /**
     * Removes all registered {@link BeanContainer}s in the current registry.
     */
    @Override
    public void removeBeanInstanceContainers() {
        resolveContainerRegistry().removeBeanInstanceContainers();
    }

    /**
     * Checks if a {@link BeanContainer} is registered for the specified {@link Scope}.
     *
     * @param scope the scope to check.
     * @return {@code true} if a container is registered for the scope, {@code false} otherwise.
     */
    @Override
    public boolean containsBeanContainer(Scope scope) {
        return resolveContainerRegistry().containsBeanContainer(scope);
    }

    /**
     * Resolves the {@link BeanContainerRegistry} associated with the current global context.
     * <p>
     * This method lazily initializes a new registry if none exists for the current context.
     * </p>
     *
     * @return the resolved {@link BeanContainerRegistry}.
     */
    private BeanContainerRegistry resolveContainerRegistry() {
        return containerRegistries.computeIfAbsent(Globals.get(), key -> defaultContainerRegistry.createObject());
    }

}
