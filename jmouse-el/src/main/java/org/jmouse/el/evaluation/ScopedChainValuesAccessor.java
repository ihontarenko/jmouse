package org.jmouse.el.evaluation;

import org.jmouse.core.bind.AbstractPropertyValuesAccessor;
import org.jmouse.core.bind.PropertyValuesAccessor;

public class ScopedChainValuesAccessor extends AbstractPropertyValuesAccessor {

    /**
     * Constructs an {@link AbstractPropertyValuesAccessor} with the given source object.
     *
     * @param chain the source object to wrap
     */
    public ScopedChainValuesAccessor(ScopedChain chain) {
        super(chain);
    }

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by name.
     *
     * @param name the name of the nested data source
     * @return the nested {@link PropertyValuesAccessor}
     */
    @Override
    public PropertyValuesAccessor get(String name) {
        ScopedChain chain = asType(ScopedChain.class);
        Object      value = null;

        if (chain.contains(name)) {
            value = chain.getValue(name);
        }

        return PropertyValuesAccessor.wrap(value);
    }

    /**
     * Retrieves a nested {@link PropertyValuesAccessor} by index.
     *
     * @param index the index of the nested data source
     * @return the nested {@link PropertyValuesAccessor}
     */
    @Override
    public PropertyValuesAccessor get(int index) {
        return null;
    }

}
