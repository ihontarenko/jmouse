package org.jmouse.core.bind.descriptor.structured.map;

import org.jmouse.core.bind.descriptor.AbstractIntrospector;
import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanPropertyIntrospector;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.util.Map;

/**
 * Introspector for analyzing and describing properties of a {@link Map} entry.
 * <p>
 * This class extends {@link AbstractIntrospector} to provide metadata extraction for
 * key-value pairs within a map, supporting property accessor methods.
 * </p>
 *
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class MapPropertyIntrospector<K, V>
        extends AbstractIntrospector<MapPropertyData<K, V>, MapPropertyIntrospector<K, V>, Map<K, V>, MapPropertyDescriptor<K, V>> {

    /**
     * Constructs a {@code MapPropertyIntrospector} for the given map.
     *
     * @param target the map to introspect
     */
    protected MapPropertyIntrospector(Map<K, V> target) {
        super(target);
    }

    /**
     * Sets the property name.
     *
     * @return this introspector instance (not implemented)
     */
    @Override
    public MapPropertyIntrospector<K, V> name() {
        return null;
    }

    /**
     * Sets the type descriptor of the map keys.
     *
     * @param type the key type descriptor
     * @return this introspector instance
     */
    public MapPropertyIntrospector<K, V> keyType(ClassTypeDescriptor type) {
        container.setKeyType(type);
        return self();
    }

    /**
     * Sets the type descriptor of the map values.
     *
     * @param type the value type descriptor
     * @return this introspector instance
     */
    public MapPropertyIntrospector<K, V> valueType(ClassTypeDescriptor type) {
        container.setValueType(type);
        return self();
    }

    /**
     * Sets the getter accessor for retrieving values from the map.
     *
     * @param key the key for retrieving values
     * @return this introspector instance
     */
    public MapPropertyIntrospector<K, V> getter(K key) {
        container.setGetter(Getter.ofMap(key));
        return self();
    }

    /**
     * Sets the setter accessor for inserting values into the map.
     *
     * @param key the key for inserting values
     * @return this introspector instance
     */
    public MapPropertyIntrospector<K, V> setter(K key) {
        container.setSetter(Setter.ofMap(key));
        return self();
    }

    /**
     * Sets the type descriptor for the introspected property.
     *
     * @param type the property type descriptor
     * @return this introspector instance
     */
    public MapPropertyIntrospector<K, V> type(ClassTypeDescriptor type) {
        return self();
    }

    /**
     * Sets the owner descriptor for the property.
     *
     * @param descriptor the MapDescriptor descriptor
     * @return this introspector instance
     */
    public MapPropertyIntrospector<K, V> owner(MapDescriptor<K, V> descriptor) {
        container.setOwner(descriptor);
        return self();
    }

    /**
     * Performs descriptor on the map property.
     *
     * @return this introspector instance
     */
    @Override
    public MapPropertyIntrospector<K, V> introspect() {
        return self();
    }

    /**
     * Creates a {@link MapPropertyDescriptor} representing the introspected map property.
     *
     * @return a new {@link MapPropertyDescriptor} instance
     */
    @Override
    public MapPropertyDescriptor<K, V> toDescriptor() {
        return getCachedDescriptor(() -> new MapPropertyDescriptor<>(this, container));
    }

    /**
     * Creates a container for storing descriptor metadata of the given map.
     *
     * @param target the map to analyze
     * @return a new {@link MapPropertyData} instance
     */
    @Override
    public MapPropertyData<K, V> getContainerFor(Map<K, V> target) {
        return new MapPropertyData<>(target);
    }
}
