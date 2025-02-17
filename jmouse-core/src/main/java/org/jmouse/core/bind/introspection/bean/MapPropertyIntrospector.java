package org.jmouse.core.bind.introspection.bean;

import java.util.Map;

public class MapPropertyIntrospector<K, V> extends PropertyIntrospector<MapPropertyIntrospector<K, V>, Map<K, V>, MapPropertyDescriptor<K, V>> {

    protected MapPropertyIntrospector(Map<K, V> target) {
        super(target);
    }

    @Override
    public MapPropertyIntrospector<K, V> name() {
        return null;
    }

    @Override
    public MapPropertyDescriptor<K, V> toDescriptor() {
        return null;
    }

    @Override
    public PropertyData<Map<K, V>> getContainerFor(Map<K, V> target) {
        return null;
    }

}
