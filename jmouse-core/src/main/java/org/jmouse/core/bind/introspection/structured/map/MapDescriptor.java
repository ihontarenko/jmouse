package org.jmouse.core.bind.introspection.structured.map;

import org.jmouse.core.bind.introspection.AbstractDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectData;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;

import java.util.Map;

/**
 * A descriptor for a {@link Map} structure that provides metadata about its key-value pairs.
 * <p>
 * This descriptor encapsulates introspection logic for maps, allowing dynamic inspection
 * of entries as properties. It extends {@link AbstractDescriptor} to integrate with
 * the structured introspection system.
 * </p>
 *
 * @param <K> the type of the keys in the map
 * @param <V> the type of the values in the map
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class MapDescriptor<K, V>
        extends AbstractDescriptor<Map<K, V>, ObjectData<Map<K, V>>, MapIntrospector<K, V>>
        implements ObjectDescriptor<Map<K, V>> {

    /**
     * Constructs a {@code MapDescriptor} with the provided introspector and container.
     *
     * @param introspector the introspector used to analyze the map
     * @param container    the container holding the map data
     */
    public MapDescriptor(MapIntrospector<K, V> introspector, ObjectData<Map<K, V>> container) {
        super(introspector, container);
    }

    /**
     * Returns the introspector responsible for analyzing the map.
     *
     * @return the associated {@link MapIntrospector}
     */
    @Override
    public MapIntrospector<K, V> toIntrospector() {
        return introspector;
    }

    /**
     * Retrieves the type descriptor representing this map.
     *
     * @return the {@link ClassTypeDescriptor} describing the map type
     */
    @Override
    public ClassTypeDescriptor getType() {
        return container.getType();
    }

    /**
     * Sets the type descriptor for this map.
     *
     * @param type the {@link ClassTypeDescriptor} to set
     */
    @Override
    public void setType(ClassTypeDescriptor type) {
        container.setType(type);
    }

    /**
     * Retrieves the properties representing the entries in the map.
     *
     * @return a map of {@link PropertyDescriptor} instances, where keys are property names
     */
    @Override
    public Map<String, PropertyDescriptor<Map<K, V>>> getProperties() {
        return container.getProperties();
    }

    /**
     * Returns a string representation of this descriptor.
     *
     * @return a formatted string representation of the map descriptor
     */
    @Override
    public String toString() {
        return "[MP]: " + getType().getJavaType();
    }
}
