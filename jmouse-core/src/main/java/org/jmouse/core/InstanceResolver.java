package org.jmouse.core;

/**
 * 🧩 Functional contract for resolving instances by their {@link Class} type.
 * <p>
 * Used as a lightweight abstraction over dependency lookup mechanisms —
 * for example, within bean factories, service registries, or proxy creators.
 * </p>
 *
 * <h3>Example ⚙️</h3>
 * <pre>{@code
 * InstanceResolver<Service> resolver = type -> new ServiceImpl();
 * Service service = resolver.resolve(ServiceImpl.class);
 * }</pre>
 *
 * @param <T> the base type of the instance to resolve
 */
@FunctionalInterface
public interface InstanceResolver<T> {

    /**
     * 🔍 Resolves (creates or retrieves) an instance for the given type.
     *
     * @param type the target class to resolve
     * @return a resolved instance (never {@code null} if successful)
     */
    T resolve(Class<? extends T> type);
}
