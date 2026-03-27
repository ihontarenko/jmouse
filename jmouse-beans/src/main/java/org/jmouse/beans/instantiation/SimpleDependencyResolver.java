package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDependency;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolutionStrategies;
import org.jmouse.beans.resolve.BeanResolutionStrategy;
import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.AnnotatedElement;

public class SimpleDependencyResolver implements DependencyResolver {

    private final BeanResolutionStrategy strategy;

    public SimpleDependencyResolver() {
        this(BeanResolutionStrategies.defaultStrategy());
    }

    public SimpleDependencyResolver(BeanResolutionStrategy strategy) {
        this.strategy = strategy;
    }

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
