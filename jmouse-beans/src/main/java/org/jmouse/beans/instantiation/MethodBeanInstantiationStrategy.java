package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanInstantiationException;
import org.jmouse.beans.BeanInstantiationType;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.BeanDependency;
import org.jmouse.beans.definition.MethodBeanDefinition;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.List;

/**
 * A strategy for instantiating beans using factory methods.
 * <p>
 * This strategy resolves dependencies for the factory method's parameters,
 * retrieves or creates the factory structured, and invokes the method to create the structured.
 * <p>
 * Example usage:
 * <pre>{@code
 * MethodBeanDefinition definition = new MethodBeanDefinition("userService", UserService.class);
 * definition.setFactoryMethod(UserFactory.class.getMethod("createUserService", UserRepository.class));
 * definition.setFactoryObject(new UserFactory());
 * definition.getBeanDependencies().add(new SimpleBeanDependency(UserRepository.class, "userRepository"));
 *
 * MethodBeanInstantiationStrategy strategy = new MethodBeanInstantiationStrategy();
 * Object userService = strategy.create(definition, beanContext);
 * }</pre>
 */
public class MethodBeanInstantiationStrategy extends AbstractBeanInstantiationStrategy {

    /**
     * Creates a structured instance using a factory method specified in the {@link MethodBeanDefinition}.
     * <p>
     * This method resolves dependencies required for the factory method, retrieves or creates
     * the factory structured, and invokes the method to create the structured.
     *
     * @param definition the structured definition describing how the structured should be created
     * @param context    the {@link BeanContext} for resolving dependencies and factory objects
     * @return the instantiated structured structured
     * @throws BeanInstantiationException if dependency resolution, factory structured retrieval,
     *                                    or method invocation fails
     */
    @Override
    public Object create(BeanDefinition definition, BeanContext context) {
        MethodBeanDefinition beanDefinition = (MethodBeanDefinition) definition;
        Method               factoryMethod  = beanDefinition.getFactoryMethod();
        Object               factoryObject  = resolveFactoryBean(beanDefinition, context);

        List<BeanDependency> dependencies = beanDefinition.getBeanDependencies();
        Object[]             arguments    = new Object[0];

        if (!dependencies.isEmpty()) {
            try {
                arguments = resolveDependencies(dependencies, context);
            } catch (Exception exception) {
                throw new BeanInstantiationException(
                        "Failed to create structured via method strategy for structured type: %s"
                                .formatted(definition.getBeanClass()), exception);
            }
        }

        return Reflections.invokeMethod(factoryObject, factoryMethod, arguments);
    }

    /**
     * Resolves or retrieves the factory structured for the given {@link MethodBeanDefinition}.
     * <p>
     * If the factory structured is not explicitly set, it is retrieved from the parent structured definition
     * in the {@link BeanContext}.
     *
     * @param definition the method-based structured definition
     * @param context    the {@link BeanContext} to resolve the factory structured
     * @return the factory structured to invoke the method on
     */
    private Object resolveFactoryBean(MethodBeanDefinition definition, BeanContext context) {
        Object factoryBean = definition.getFactoryObject();

        if (factoryBean == null) {
            BeanDefinition parent = definition.getParentDefinition();

            factoryBean = context.getBean(parent.getBeanName());

            // Set the resolved factory structured for all child definitions of the parent
            for (BeanDefinition childDefinition : parent.getChildrenDefinitions()) {
                if (childDefinition instanceof MethodBeanDefinition methodBeanDefinition) {
                    methodBeanDefinition.setFactoryObject(factoryBean);
                }
            }
        }

        return factoryBean;
    }

    /**
     * Determines if this strategy supports the given {@link BeanDefinition}.
     * <p>
     * This strategy supports definitions with an instantiation type of {@link BeanInstantiationType#FACTORY_METHOD}.
     *
     * @param definition the structured definition to evaluate
     * @return {@code true} if the definition is supported, otherwise {@code false}
     */
    @Override
    public boolean supports(BeanDefinition definition) {
        return definition.getInstantiationType() == BeanInstantiationType.FACTORY_METHOD;
    }
}
