package org.jmouse.core.mapping.access;

import org.jmouse.core.Verify;

import java.util.Map;

public final class MapSourcePropertyReader implements SourcePropertyReader {

    private final Map<?, ?> map;

    public MapSourcePropertyReader(Map<?, ?> map) {
        this.map = Verify.nonNull(map, "map");
    }

    @Override
    public boolean has(String name) {
        return map.containsKey(name);
    }

    @Override
    public Object read(String name) {
        return map.get(name);
    }

    @Override
    public Class<?> sourceType() {
        return map.getClass();
    }

}
