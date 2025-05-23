package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDependency;

import java.util.*;

/**
 * An abstract base class for {@link BeanInstantiationStrategy} implementations.
 * Provides a utility method for resolving dependencies from a {@link BeanContext}.
 * <p>
 * Example usage in a concrete strategy:
 * <pre>{@code
 * public class ConstructorBeanInstantiationStrategy extends AbstractBeanInstantiationStrategy {
 *     @Override
 *     public Object create(BeanDefinition definition, BeanContext context) {
 *         List<BeanDependency> dependencies = definition.getBeanDependencies();
 *         Object[] arguments = resolveDependencies(dependencies, context);
 *         return Reflections.instantiate(definition.getConstructor(), arguments);
 *     }
 *
 *     @Override
 *     public boolean supports(BeanDefinition definition) {
 *         return definition.getInstantiationType() == BeanInstantiationType.CONSTRUCTOR;
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractBeanInstantiationStrategy implements BeanInstantiationStrategy, DependencyResolver.Aware {

    private DependencyResolver dependencyResolver;

    @Override
    public void setDependencyResolver(DependencyResolver resolver) {
        this.dependencyResolver = resolver;
    }

    @Override
    public DependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    /**
     * Resolves a list of {@link BeanDependency} instances into actual bean objects
     * by querying the provided {@link BeanContext}.
     *
     * @param dependencies a list of dependencies to resolve
     * @param context      the {@link BeanContext} used to resolve beans
     * @return an array of resolved dependency objects
     */
    protected Object[] resolveDependencies(List<BeanDependency> dependencies, BeanContext context) {
        List<Object> arguments = new ArrayList<>();

        for (BeanDependency dependency : dependencies) {
            arguments.add(dependencyResolver.resolve(dependency, context));
        }

        return arguments.toArray(Object[]::new);
    }
}
