package org.jmouse.testing_ground.beans;

import org.jmouse.testing_ground.beans.definition.BeanDefinition;
import org.jmouse.testing_ground.beans.definition.BeanDefinitionException;

/**
 * Resolves the {@link Scope} of a bean based on its {@link BeanDefinition}.
 * <p>
 * This implementation of {@link ScopeResolver} retrieves the scope of a bean
 * by looking up its definition in the provided {@link BeanDefinitionContainer}.
 * </p>
 */
public class BeanDefinitionScopeResolver implements ScopeResolver {

    private final BeanDefinitionContainer definitionContainer;

    /**
     * Creates a new {@code BeanDefinitionScopeResolver} with the specified definition container.
     *
     * @param definitionContainer the container holding {@link BeanDefinition}s.
     */
    public BeanDefinitionScopeResolver(BeanDefinitionContainer definitionContainer) {
        this.definitionContainer = definitionContainer;
    }

    /**
     * Resolves the {@link Scope} of a bean based on its name.
     * <p>
     * This method retrieves the {@link BeanDefinition} for the specified name
     * and extracts its scope. If the definition is not found, an exception is thrown.
     * </p>
     *
     * @param name the name of the bean whose scope is to be resolved.
     * @return the {@link Scope} associated with the bean.
     * @throws BeanDefinitionException if no bean definition is found for the given name.
     */
    @Override
    public Scope resolveScope(String name) {
        BeanDefinition beanDefinition = definitionContainer.getDefinition(name);

        if (beanDefinition == null) {
            throw new BeanDefinitionException(
                    "Failed to resolve scope. No bean definition found for: '%s'".formatted(name));
        }

        return beanDefinition.getScope();
    }
}
