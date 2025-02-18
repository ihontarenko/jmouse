package org.jmouse.core.bind.introspection.structured.map;

import org.jmouse.core.bind.introspection.AbstractIntrospector;
import org.jmouse.core.bind.introspection.structured.ObjectData;
import org.jmouse.core.bind.introspection.structured.ObjectIntrospector;

import java.util.Map;

public class MapIntrospector<K, V> extends AbstractIntrospector<ObjectData<Map<K, V>>, MapIntrospector<K, V>, Map<K, V>, MapDescriptor<K, V>> {

    protected MapIntrospector(Map<K, V> target) {
        super(target);
    }

    @Override
    public MapIntrospector<K, V> name() {
        return null;
    }

    @Override
    public MapIntrospector<K, V> introspect() {
        return null;
    }

    @Override
    public MapDescriptor<K, V> toDescriptor() {
        return getCachedDescriptor(() -> new MapDescriptor<>(this, container));
    }

    @Override
    public ObjectData<Map<K, V>> getContainerFor(Map<K, V> target) {
        return null;
    }

}
