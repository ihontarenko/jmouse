package org.jmouse.core.bind.introspection.bean;

import java.util.Map;

public class MapDescriptor<K, V> extends ObjectDescriptor<Map<K, V>, MapIntrospector<K, V>> {

    public MapDescriptor(MapIntrospector<K, V> introspector, ObjectData<Map<K, V>> container) {
        super(introspector, container);
    }

    @Override
    public MapIntrospector<K, V> toIntrospector() {
        return introspector;
    }

}
