package org.jmouse.beans;

import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.BeanDefinitionException;
import org.jmouse.beans.definition.DuplicateBeanDefinitionException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple implementation of the {@link BeanDefinitionContainer} interface.
 * <p>
 * This container manages {@link BeanDefinition} objects and provides thread-safe
 * methods for registering, retrieving, and managing bean definitions.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BeanDefinitionContainer container = new DefaultBeanDefinitionContainer();
 * BeanDefinition definition = new DefaultBeanDefinition("userService", UserService.class);
 *
 * container.registerDefinition(definition);
 * BeanDefinition retrievedDefinition = container.getDefinition("userService");
 *
 * System.out.println("Bean name: " + retrievedDefinition.getBeanName());
 * }</pre>
 */
public class DefaultBeanDefinitionContainer implements BeanDefinitionContainer {

    private final Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();

    /**
     * Registers a {@link BeanDefinition} in the container.
     *
     * @param definition the definition to register
     */
    @Override
    public void registerDefinition(BeanDefinition definition) {
        if (definition == null) {
            throw new BeanDefinitionException("Bean definition is required but NULL passed");
        }

        String beanName = definition.getBeanName();

        if (containsDefinition(beanName)) {
            throw new DuplicateBeanDefinitionException(definition);
        }

        definitions.put(beanName, definition);
    }

    /**
     * Retrieves a {@link BeanDefinition} by its name.
     *
     * @param name the name of the bean definition
     * @return the bean definition corresponding to the given name,
     * or {@code null} if no definition exists for that name
     */
    @Override
    public BeanDefinition getDefinition(String name) {
        return definitions.get(name);
    }

    /**
     * Checks if a {@link BeanDefinition} with the given name exists in the container.
     *
     * @param name the name of the bean definition
     * @return {@code true} if the container contains a definition with the specified name,
     * {@code false} otherwise
     */
    @Override
    public boolean containsDefinition(String name) {
        return definitions.containsKey(name);
    }

    /**
     * Retrieves all registered {@link BeanDefinition}s in the container.
     *
     * @return a collection of all registered bean definitions.
     */
    @Override
    public Collection<BeanDefinition> getDefinitions() {
        return new ArrayList<>(definitions.values());
    }

    @Override
    public String toString() {
        return "Definitions: [%d]".formatted(definitions.size());
    }
}
