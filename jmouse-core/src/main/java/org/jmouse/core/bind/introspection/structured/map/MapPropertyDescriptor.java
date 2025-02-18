package org.jmouse.core.bind.introspection.structured.map;

import org.jmouse.core.bind.introspection.AbstractDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyData;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.util.Map;

public class MapPropertyDescriptor<K, V>
        extends AbstractDescriptor<Map<K, V>, PropertyData<Map<K, V>>, MapPropertyIntrospector<K, V>>
        implements PropertyDescriptor<Map<K, V>> {

    protected MapPropertyDescriptor(MapPropertyIntrospector<K, V> introspector, PropertyData<Map<K, V>> container) {
        super(introspector, container);
    }

    @Override
    public MapPropertyIntrospector<K, V> toIntrospector() {
        return introspector;
    }

    @Override
    public ObjectDescriptor<Map<K, V>> getOwner() {
        return container.getOwner();
    }

    @Override
    public Getter<Map<K, V>, Object> getGetter() {
        return container.getGetter();
    }

    @Override
    public void setGetter(Getter<Map<K, V>, Object> getter) {
        container.setGetter(getter);
    }

    @Override
    public Setter<Map<K, V>, Object> getSetter() {
        return container.getSetter();
    }

    @Override
    public void setSetter(Setter<Map<K, V>, Object> setter) {
        container.setSetter(setter);
    }

}
