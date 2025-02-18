package org.jmouse.core.bind.introspection.structured.map;

import org.jmouse.core.bind.introspection.AbstractIntrospector;
import org.jmouse.core.bind.introspection.structured.PropertyData;

import java.util.Map;

public class MapPropertyIntrospector<K, V>
        extends AbstractIntrospector<PropertyData<Map<K, V>>, MapPropertyIntrospector<K, V>, Map<K, V>, MapPropertyDescriptor<K, V>> {

    protected MapPropertyIntrospector(Map<K, V> target) {
        super(target);
    }

    @Override
    public MapPropertyIntrospector<K, V> name() {
        return null;
    }

    @Override
    public MapPropertyIntrospector<K, V> introspect() {
        return null;
    }

    @Override
    public MapPropertyDescriptor<K, V> toDescriptor() {
        return getCachedDescriptor(() -> new MapPropertyDescriptor<>(this, container));
    }

    @Override
    public PropertyData<Map<K, V>> getContainerFor(Map<K, V> target) {
        return new PropertyData<>(target);
    }

}
