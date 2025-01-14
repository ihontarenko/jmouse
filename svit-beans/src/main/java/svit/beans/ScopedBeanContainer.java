package svit.beans;

/**
 * A composite interface that combines the functionalities of {@link BeanContainer} and {@link BeanContainerRegistry}.
 * <p>
 * The {@code ScopedBeanContainer} serves as a unified API for managing beans and their corresponding containers
 * across different scopes. This allows for seamless registration, retrieval, and lifecycle management of beans,
 * regardless of their scope (e.g., singleton, prototype, or custom-defined scopes).
 * </p>
 *
 * @see BeanContainer
 * @see BeanContainerRegistry
 */
public interface ScopedBeanContainer extends BeanContainer, BeanContainerRegistry {

}
