package org.jmouse.core.bind.introspection.structured.map;

import org.jmouse.core.bind.introspection.AbstractDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectData;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;

import java.util.Map;

public class MapDescriptor<K, V>
        extends AbstractDescriptor<Map<K, V>, ObjectData<Map<K, V>>, MapIntrospector<K, V>> implements ObjectDescriptor<Map<K, V>> {

    public MapDescriptor(MapIntrospector<K, V> introspector, ObjectData<Map<K, V>> container) {
        super(introspector, container);
    }

    @Override
    public MapIntrospector<K, V> toIntrospector() {
        return introspector;
    }

    @Override
    public ClassTypeDescriptor getType() {
        return container.getType();
    }

    @Override
    public void setType(ClassTypeDescriptor type) {
        container.setType(type);
    }

    @Override
    public Map<String, PropertyDescriptor<Map<K, V>>> getProperties() {
        return container.getProperties();
    }

}
