package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDependency;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolutionStrategies;
import org.jmouse.beans.resolve.BeanResolutionStrategy;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.AnnotatedElement;

/**
 * Default {@link DependencyResolver} based on {@link BeanResolutionStrategy}. 🧩
 *
 * <p>Builds a {@link BeanResolutionRequest} from {@link BeanDependency} metadata and delegates resolution.</p>
 */
public class SimpleDependencyResolver implements DependencyResolver {

    private final BeanResolutionStrategy strategy;

    /**
     * Creates resolver with default bean resolution strategy.
     */
    public SimpleDependencyResolver() {
        this(BeanResolutionStrategies.defaultStrategy());
    }

    /**
     * Creates resolver with custom bean resolution strategy.
     *
     * @param strategy resolution strategy
     */
    public SimpleDependencyResolver(BeanResolutionStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Resolves the given dependency against the provided bean context.
     *
     * @param dependency bean dependency metadata
     * @param context    bean context
     * @return resolved dependency value
     */
    @Override
    public Object resolve(BeanDependency dependency, BeanContext context) {
        InferredType          javaType = dependency.javaType();
        String                beanName = dependency.name();
        AnnotatedElement      element  = dependency.dependant();
        BeanResolutionRequest request  = BeanResolutionRequest.forDependency(
                context, javaType, beanName, element, true);
        return strategy.resolve(request);
    }

}