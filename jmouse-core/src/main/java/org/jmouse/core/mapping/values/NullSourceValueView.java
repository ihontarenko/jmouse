package org.jmouse.core.mapping.values;

import org.jmouse.core.bind.PropertyPath;

import java.util.List;

final class NullSourceValueView implements SourceValueView {

    static final NullSourceValueView INSTANCE = new NullSourceValueView();

    private NullSourceValueView() {}

    @Override
    public ValueKind kind() {
        return ValueKind.NULL;
    }

    @Override
    public Object raw() {
        return null;
    }

    @Override
    public SourceValueView navigate(PropertyPath path) {
        return this;
    }

    @Override
    public Iterable<PropertyEntry> entries() {
        return List.of();
    }

    @Override
    public Iterable<SourceValueView> elements() {
        return List.of();
    }
}
