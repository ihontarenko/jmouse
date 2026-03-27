package org.jmouse.beans.resolve.support;

import org.jmouse.beans.resolve.BeanResolutionStrategy;
import org.jmouse.core.Delegate;

/**
 * Base resolver that delegates nested resolution to a {@link BeanResolutionStrategy}. 🔗
 *
 * <p>Used when resolver needs to resolve inner dependencies via strategy.</p>
 */
public abstract class AbstractDelegatingBeanResolver extends AbstractBeanResolver implements Delegate<BeanResolutionStrategy> {

    private BeanResolutionStrategy strategy;

    /**
     * Creates resolver with delegation strategy.
     *
     * @param strategy resolution strategy
     */
    protected AbstractDelegatingBeanResolver(BeanResolutionStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns current resolution strategy.
     */
    @Override
    public BeanResolutionStrategy getDelegate() {
        return strategy;
    }

    /**
     * Updates resolution strategy.
     */
    @Override
    public void setDelegate(BeanResolutionStrategy delegate) {
        this.strategy = delegate;
    }

}