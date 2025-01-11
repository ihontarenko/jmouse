package svit.container.instantiation;

import svit.container.BeanContext;
import svit.container.BeanInstantiationType;
import svit.container.definition.BeanDependency;
import svit.container.BeanInstantiationException;
import svit.container.definition.ConstructorBeanDefinition;
import svit.container.definition.BeanDefinition;
import svit.reflection.Reflections;

import java.util.List;

/**
 * A strategy for instantiating beans using constructors.
 * <p>
 * This strategy resolves dependencies for the constructor parameters of a bean
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
     * Creates a bean instance using a constructor specified in the {@link ConstructorBeanDefinition}.
     * <p>
     * This method resolves dependencies required for the constructor, if any, and
     * then uses reflection to invoke the constructor.
     *
     * @param definition the bean definition describing how the bean should be created
     * @param context    the {@link BeanContext} for resolving dependencies
     * @return the instantiated bean object
     * @throws BeanInstantiationException if dependency resolution or instantiation fails
     */
    @Override
    public Object create(BeanDefinition definition, BeanContext context) {
        ConstructorBeanDefinition beanDefinition = (ConstructorBeanDefinition) definition;
        List<BeanDependency> dependencies = beanDefinition.getBeanDependencies();
        Object[] arguments = new Object[0];

        if (!dependencies.isEmpty()) {
            try {
                arguments = resolveDependencies(dependencies, context);
            } catch (RuntimeException exception) {
                throw new BeanInstantiationException(
                        "Failed to create bean via constructor strategy for bean type: " + definition.getBeanClass(),
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
     * @param definition the bean definition to evaluate
     * @return {@code true} if the definition is supported, otherwise {@code false}
     */
    @Override
    public boolean supports(BeanDefinition definition) {
        return definition.getInstantiationType() == BeanInstantiationType.CONSTRUCTOR;
    }
}
