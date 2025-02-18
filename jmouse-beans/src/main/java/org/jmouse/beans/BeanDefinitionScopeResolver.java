package org.jmouse.beans;

import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.BeanDefinitionException;

/**
 * Resolves the {@link Scope} of a structured based on its {@link BeanDefinition}.
 * <p>
 * This implementation of {@link ScopeResolver} retrieves the scope of a structured
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
     * Resolves the {@link Scope} of a structured based on its name.
     * <p>
     * This method retrieves the {@link BeanDefinition} for the specified name
     * and extracts its scope. If the definition is not found, an exception is thrown.
     * </p>
     *
     * @param name the name of the structured whose scope is to be resolved.
     * @return the {@link Scope} associated with the structured.
     * @throws BeanDefinitionException if no structured definition is found for the given name.
     */
    @Override
    public Scope resolveScope(String name) {
        BeanDefinition beanDefinition = definitionContainer.getDefinition(name);

        if (beanDefinition == null) {
            throw new BeanDefinitionException(
                    "Failed to resolve scope. No structured definition found for: '%s'".formatted(name));
        }

        return beanDefinition.getScope();
    }
}
