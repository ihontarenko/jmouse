package org.jmouse.core.bind.descriptor;

import java.util.Map;

public class MapDescriptor<K, V> extends ObjectDescriptor<Map<K, V>, MapDescriptor<K, V>, MapIntrospector<K, V>> {

    public MapDescriptor(MapIntrospector<K, V> introspector, ObjectData<Map<K, V>> container) {
        super(introspector, container);
    }

    @Override
    public MapIntrospector<K, V> toIntrospector() {
        return introspector;
    }

}
