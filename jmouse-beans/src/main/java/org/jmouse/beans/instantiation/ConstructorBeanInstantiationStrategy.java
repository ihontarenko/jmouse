package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanInstantiationType;
import org.jmouse.beans.definition.BeanDependency;
import org.jmouse.beans.BeanInstantiationException;
import org.jmouse.beans.definition.ConstructorBeanDefinition;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.reflection.Reflections;

import java.util.List;

/**
 * A strategy for instantiating beans using constructors.
 * <p>
 * This strategy resolves dependencies for the constructor parameters of a structured
 * and uses reflection to invoke the appropriate constructor.
 * <p>
 * Example usage:
 * <pre>{@code
 * ConstructorBeanDefinition definition = new ConstructorBeanDefinition("userService", UserService.class);
 * definition.setConstructor(UserService.class.getConstructor(UserRepository.class));
 * definition.getBeanDependencies().add(new SimpleBeanDependency(UserRepository.class, "userRepository"));
 *
 * ConstructorBeanInstantiationStrategy strategy = new ConstructorBeanInstantiationStrategy();
 * Object userService = strategy.create(definition, beanContext);
 * }</pre>
 */
public class ConstructorBeanInstantiationStrategy extends AbstractBeanInstantiationStrategy {

    /**
     * Creates a structured instance using a constructor specified in the {@link ConstructorBeanDefinition}.
     * <p>
     * This method resolves dependencies required for the constructor, if any, and
     * then uses reflection to invoke the constructor.
     *
     * @param definition the structured definition describing how the structured should be created
     * @param context    the {@link BeanContext} for resolving dependencies
     * @return the instantiated structured structured
     * @throws BeanInstantiationException if dependency resolution or instantiation fails
     */
    @Override
    public Object create(BeanDefinition definition, BeanContext context) {
        ConstructorBeanDefinition beanDefinition = (ConstructorBeanDefinition) definition;
        Object[]                  arguments      = new Object[0];
        List<BeanDependency>      dependencies   = beanDefinition.getBeanDependencies();

        if (!dependencies.isEmpty()) {
            try {
                arguments = resolveDependencies(dependencies, context);
            } catch (RuntimeException exception) {
                throw new BeanInstantiationException(
                        "Failed to create structured via constructor strategy for structured type: " + definition.getBeanClass(),
                        exception);
            }
        }

        return Reflections.instantiate(beanDefinition.getConstructor(), arguments);
    }

    /**
     * Determines if this strategy supports the given {@link BeanDefinition}.
     * <p>
     * This strategy supports definitions with an instantiation type of {@link BeanInstantiationType#CONSTRUCTOR}.
     *
     * @param definition the structured definition to evaluate
     * @return {@code true} if the definition is supported, otherwise {@code false}
     */
    @Override
    public boolean supports(BeanDefinition definition) {
        return definition.getInstantiationType() == BeanInstantiationType.CONSTRUCTOR;
    }
}
