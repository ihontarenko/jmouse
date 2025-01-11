package svit.container;

import svit.container.definition.BeanDefinition;

/**
 * An interface for managing {@link BeanDefinition} objects within a container.
 * <p>
 * For instance, you might define a bean named <em>userService</em> that references a
 * {@code com.example.services.UserService} class:
 * <pre>{@code
 * BeanDefinitionContainer container = ...;
 * BeanDefinition userServiceDefinition =
 *     new DefaultBeanDefinition("userService", com.example.services.UserService.class);
 *
 * container.registerDefinition(userServiceDefinition);
 *
 * BeanDefinition retrievedDefinition = container.getDefinition("userService");
 * System.out.println("Bean name: " + retrievedDefinition.getBeanName());
 * }</pre>
 */
public interface BeanDefinitionContainer {

    /**
     * Registers a {@link BeanDefinition} in the container.
     *
     * @param definition the definition to register
     */
    void registerDefinition(BeanDefinition definition);

    /**
     * Retrieves a {@link BeanDefinition} by its name.
     *
     * @param name the name of the bean definition
     * @return the bean definition corresponding to the given name,
     *         or {@code null} if no definition exists for that name
     */
    BeanDefinition getDefinition(String name);
}
