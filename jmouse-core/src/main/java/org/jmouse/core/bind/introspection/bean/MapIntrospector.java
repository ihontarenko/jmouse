package org.jmouse.core.bind.introspection.bean;

import java.util.Map;

public class MapIntrospector<K, V> extends ObjectIntrospector<MapIntrospector<?, ?>, Map<K, V>, MapDescriptor<K, V>> {

    protected MapIntrospector(Map<K, V> target) {
        super(target);
    }

    @Override
    public MapIntrospector<?, ?> name() {
        return null;
    }

    @Override
    public MapDescriptor<K, V> toDescriptor() {
        return getDescriptor(() -> new MapDescriptor<>(this, container));
    }

}
