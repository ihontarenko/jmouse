package org.jmouse.crawler.runtime.state;

import org.jmouse.core.context.mutable.MutableAttributesContext;
import org.jmouse.core.context.mutable.StandardMutableContextContainer;
import org.jmouse.crawler.api.DynamicAttributes;

import java.util.Map;

public class InMemoryDynamicAttributes implements DynamicAttributes {

    private final MutableAttributesContext mutableContext = new StandardMutableContextContainer();

    @Override
    public void setValue(Object key, Object value) {
        mutableContext.setValue(key, value);
    }

    @Override
    public void removeValue(Object key) {
        mutableContext.removeValue(key);
    }

    @Override
    public void clear() {
        mutableContext.clear();
    }

    @Override
    public Map<Object, Object> asMapView() {
        return mutableContext.asMapView();
    }

    @Override
    public <T> T getValue(Object key) {
        return mutableContext.getValue(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return mutableContext.containsKey(key);
    }

}
