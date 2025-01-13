package svit.beans;

/**
 * A composite interface that combines the functionalities of {@link BeanContainer} and {@link BeanContainerRegistry}.
 * <p>
 * The {@code ScopedBeanContainer} serves as a unified API for managing beans and their corresponding containers
 * across different scopes. This allows for seamless registration, retrieval, and lifecycle management of beans,
 * regardless of their scope (e.g., singleton, prototype, or custom-defined scopes).
 * </p>
 *
 * <p>Key Features:</p>
 * <ul>
 *     <li>Provides methods to retrieve and register beans by name and type.</li>
 *     <li>Manages scoped containers through {@link BeanContainerRegistry}.</li>
 *     <li>Supports custom implementations for handling additional or custom scopes.</li>
 * </ul>
 *
 * @see BeanContainer
 * @see BeanContainerRegistry
 */
public interface ScopedBeanContainer extends BeanContainer, BeanContainerRegistry {
}
