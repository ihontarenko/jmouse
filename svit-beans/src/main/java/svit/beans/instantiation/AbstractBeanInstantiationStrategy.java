package svit.beans.instantiation;

import svit.beans.BeanContext;
import svit.beans.definition.BeanDependency;

import java.util.ArrayList;
import java.util.List;

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
public abstract class AbstractBeanInstantiationStrategy implements BeanInstantiationStrategy {

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
            arguments.add(context.getBean(dependency.type(), dependency.name()));
        }

        return arguments.toArray(Object[]::new);
    }
}
