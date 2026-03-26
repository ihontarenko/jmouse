package org.jmouse.beans.resolve.support;

import org.jmouse.beans.resolve.BeanResolutionStrategy;

public abstract class AbstractDelegatingBeanResolver extends AbstractBeanResolver {

    private final BeanResolutionStrategy strategy;

    protected AbstractDelegatingBeanResolver(BeanResolutionStrategy strategy) {
        this.strategy = strategy;
    }

    protected BeanResolutionStrategy strategy() {
        return strategy;
    }
}
