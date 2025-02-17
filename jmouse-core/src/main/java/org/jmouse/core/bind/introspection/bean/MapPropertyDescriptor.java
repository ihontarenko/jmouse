package org.jmouse.core.bind.introspection.bean;

import java.util.Map;

public class MapPropertyDescriptor<K, V> extends PropertyDescriptor<Map<K, V>, MapPropertyIntrospector<K, V>> {

    protected MapPropertyDescriptor(MapPropertyIntrospector<K, V> introspector, PropertyData<Map<K, V>> container) {
        super(introspector, container);
    }
}
