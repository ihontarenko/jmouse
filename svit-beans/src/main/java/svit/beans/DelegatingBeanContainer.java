package svit.beans;

/**
 * A delegating bean container that combines the functionalities of {@link BeanContainer}
 * and {@link BeanContainerRegistry}.
 * <p>
 * This interface allows for both managing individual bean instances and registering or retrieving
 * specific {@link BeanContainer}s for different {@link Scope}s.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * DelegatingBeanContainer container = ...;
 *
 * // Registering a scoped container
 * container.registerBeanInstanceContainer(BeanScope.REQUEST, new RequestScopedBeanContainer());
 *
 * // Adding a bean to the registered container
 * container.getBeanInstanceContainer(BeanScope.REQUEST).registerBean("sessionBean", new SessionBean());
 *
 * // Retrieving the bean
 * SessionBean sessionBean = container.getBeanInstanceContainer(BeanScope.REQUEST).getBean("sessionBean");
 * }</pre>
 *
 * @see BeanContainer
 * @see BeanContainerRegistry
 */
public interface DelegatingBeanContainer extends BeanContainer, BeanContainerRegistry {

}
