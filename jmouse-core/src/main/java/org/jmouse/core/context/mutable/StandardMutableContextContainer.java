package org.jmouse.core.context.mutable;

import org.jmouse.core.context.immutable.ImmutableSnapshotContext;

import java.util.Map;

/**
 * Typical mutable container that can be used in handlers/pipelines.
 */
public final class StandardMutableContextContainer
        implements MutableArgumentsContext, MutableAttributesContext, MutableVariablesContext, MutableProcessingControl {

    private final MapBackedMutableContext values;
    private       boolean                 processingStopped;

    public StandardMutableContextContainer() {
        this.values = new MapBackedMutableContext();
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
        processingStopped = false;
    }

    @Override
    public boolean isProcessingStopped() {
        return processingStopped;
    }

    @Override
    public void stopProcessing() {
        processingStopped = true;
    }

    public ImmutableSnapshotContext createImmutableSnapshot() {
        return new ImmutableSnapshotContext(asMapView());
    }

    @Override
    public String toString() {
        return "StandardMutableContextContainer[stopped=" + processingStopped + ", values=" + asMapView() + "]";
    }
}
