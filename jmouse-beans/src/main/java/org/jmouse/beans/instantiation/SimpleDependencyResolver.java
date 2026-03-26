package org.jmouse.beans.instantiation;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDependency;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolutionStrategies;
import org.jmouse.beans.resolve.BeanResolutionStrategy;

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
        return strategy.resolve(BeanResolutionRequest.forDependency(context, dependency));
    }

}
