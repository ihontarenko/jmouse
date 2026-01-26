package org.jmouse.core.mapping.model;

import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.mapping.values.ValueKind;

import java.util.Map;
import java.util.Objects;

final class MapSourceModel implements SourceModel {

    private final Map<?, ?> map;

    MapSourceModel(Map<?, ?> map) {
        this.map = Objects.requireNonNull(map, "map");
    }

    @Override
    public Object source() {
        return map;
    }

    @Override
    public Class<?> sourceType() {
        return map.getClass();
    }

    @Override
    public ValueKind kind() {
        return ValueKind.MAP;
    }

    @Override
    public ObjectDescriptor<?> descriptor() {
        return null;
    }

    @Override
    public boolean has(String name) {
        return map.containsKey(name);
    }

    @Override
    public Object read(String name) {
        return map.get(name);
    }

}
