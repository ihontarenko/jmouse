package org.jmouse.pipeline.context;

import org.jmouse.core.Streamable;
import org.jmouse.core.context.beans.BeanLookup;
import org.jmouse.core.context.beans.MissingBeanProviderException;
import org.jmouse.core.context.keys.ContextKeyResolver;
import org.jmouse.core.context.keys.UserClassContextKeyResolver;
import org.jmouse.core.context.mutable.MutableArgumentsContext;
import org.jmouse.core.context.mutable.StandardMutableContextContainer;
import org.jmouse.core.context.result.MapBackedMutableResultContext;
import org.jmouse.core.context.result.MutableResultContext;

import java.util.Map;

public final class DefaultPipelineContext implements PipelineContext {

    private final StandardMutableContextContainer values;
    private final MapBackedMutableResultContext   result;
    private final ContextKeyResolver              keyResolver;

    private BeanLookup beanLookup;

    public DefaultPipelineContext() {
        this(null);
    }

    public DefaultPipelineContext(BeanLookup beanLookup) {
        this.keyResolver = new UserClassContextKeyResolver();
        this.values = new StandardMutableContextContainer();
        this.result = new MapBackedMutableResultContext();
        this.beanLookup = beanLookup;
    }

    @Override
    public BeanLookup getBeanLookup() {
        if (beanLookup == null) {
            throw new MissingBeanProviderException(
                    "BeanProvider is not configured for this pipeline context."
            );
        }
        return beanLookup;
    }

    public void setBeanLookup(BeanLookup beanLookup) {
        this.beanLookup = beanLookup;
    }

    public void setArgument(Object argument) {
        Object key = keyResolver.resolveKeyFor(argument);
        values.setArgument(key, argument);
    }

    public void setArguments(Object... arguments) {
        Streamable.of(arguments).forEach(this::setArgument);
    }

    @Override
    public MutableResultContext getResultContext() {
        return result;
    }

    @Override
    public MutableArgumentsContext getArgumentsContext() {
        return values;
    }

    @Override
    public Map<Object, Object> asMapView() {
        return values.asMapView();
    }

    @Override
    public <T> T getValue(Object key) {
        return values.getValue(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    public boolean isProcessingStopped() {
        return values.isProcessingStopped();
    }

    @Override
    public void setValue(Object key, Object value) {
        values.setValue(key, value);
    }

    @Override
    public void removeValue(Object key) {
        values.removeValue(key);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public String toString() {
        return "DefaultPipelineContext[stopped=" + isProcessingStopped()
                + ", values=" + asMapView()
                + ", hasErrors=" + result.hasErrors()
                + "]";
    }
}
