package svit.beans;

import svit.beans.definition.BeanDefinition;

/**
 * An interface for managing {@link BeanDefinition} objects within a container.
 * <p>
 * This interface allows the registration, retrieval, and management of bean definitions.
 * For example, you can register a bean definition and then retrieve it by its name:
 * <pre>{@code
 * BeanDefinitionContainer container = ...;
 * BeanDefinition userServiceDefinition =
 *     new DefaultBeanDefinition("userService", com.example.services.UserService.class);
 *
 * container.registerDefinition(userServiceDefinition);
 *
 * if (container.containsDefinition("userService")) {
 *     BeanDefinition retrievedDefinition = container.getDefinition("userService");
 *     System.out.println("Bean name: " + retrievedDefinition.getBeanName());
 * }
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

    /**
     * Checks if a {@link BeanDefinition} with the given name exists in the container.
     *
     * @param name the name of the bean definition
     * @return {@code true} if the container contains a definition with the specified name,
     *         {@code false} otherwise
     */
    boolean containsDefinition(String name);
}
