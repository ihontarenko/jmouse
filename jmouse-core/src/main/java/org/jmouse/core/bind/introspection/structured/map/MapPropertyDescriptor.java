package org.jmouse.core.bind.introspection.structured.map;

import org.jmouse.core.bind.introspection.AbstractDescriptor;
import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.structured.ObjectDescriptor;
import org.jmouse.core.bind.introspection.structured.PropertyDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.util.Map;

/**
 * Represents a descriptor for a map property, encapsulating metadata
 * about the key and value types, accessors, and ownership.
 * <p>
 * This descriptor extends {@link AbstractDescriptor} and implements
 * {@link PropertyDescriptor}, providing detailed introspection capabilities
 * for key-value properties within a map.
 * </p>
 *
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class MapPropertyDescriptor<K, V>
        extends AbstractDescriptor<Map<K, V>, MapPropertyData<K, V>, MapPropertyIntrospector<K, V>>
        implements PropertyDescriptor<Map<K, V>> {

    /**
     * Constructs a {@code MapPropertyDescriptor} with the given introspector and container.
     *
     * @param introspector the introspector used for analyzing the map property
     * @param container    the container holding the metadata
     */
    protected MapPropertyDescriptor(MapPropertyIntrospector<K, V> introspector, MapPropertyData<K, V> container) {
        super(introspector, container);
    }

    /**
     * Returns the type descriptor of the map key.
     *
     * @return the key type descriptor
     */
    public ClassTypeDescriptor getKeyType() {
        return container.getKeyType();
    }

    /**
     * Sets the type descriptor of the map key.
     *
     * @param keyType the key type descriptor to set
     */
    public void setKeyType(ClassTypeDescriptor keyType) {
        container.setKeyType(keyType);
    }

    /**
     * Returns the type descriptor of the map value.
     *
     * @return the value type descriptor
     */
    public ClassTypeDescriptor getValueType() {
        return container.getValueType();
    }

    /**
     * Sets the type descriptor of the map value.
     *
     * @param valueType the value type descriptor to set
     */
    public void setValueType(ClassTypeDescriptor valueType) {
        container.setValueType(valueType);
    }

    /**
     * Returns the introspector associated with this descriptor.
     *
     * @return the {@link MapPropertyIntrospector} instance
     */
    @Override
    public MapPropertyIntrospector<K, V> toIntrospector() {
        return introspector;
    }

    /**
     * Returns the type descriptor for the property.
     * <p>
     * This method returns the type descriptor for the value type,
     * as it represents the data stored in the map.
     * </p>
     *
     * @return the value type descriptor
     */
    @Override
    public ClassTypeDescriptor getType() {
        return getValueType();
    }

    /**
     * Sets the type descriptor for the property.
     * <p>
     * This method delegates the setting of the value type descriptor.
     * </p>
     *
     * @param type the type descriptor to set
     */
    @Override
    public void setType(ClassTypeDescriptor type) {
        setValueType(type);
    }

    /**
     * Returns the owner descriptor of this property.
     *
     * @return the owning {@link ObjectDescriptor} instance
     */
    @Override
    public ObjectDescriptor<Map<K, V>> getOwner() {
        return container.getOwner();
    }

    /**
     * Returns the getter for accessing values in the map.
     *
     * @return the getter function
     */
    @Override
    public Getter<Map<K, V>, Object> getGetter() {
        return container.getGetter();
    }

    /**
     * Sets the getter function for retrieving values from the map.
     *
     * @param getter the getter function to set
     */
    @Override
    public void setGetter(Getter<Map<K, V>, ?> getter) {
        container.setGetter(getter);
    }

    /**
     * Returns the setter for inserting values into the map.
     *
     * @return the setter function
     */
    @Override
    public Setter<Map<K, V>, Object> getSetter() {
        return container.getSetter();
    }

    /**
     * Sets the setter function for inserting values into the map.
     *
     * @param setter the setter function to set
     */
    @Override
    public void setSetter(Setter<Map<K, V>, ?> setter) {
        container.setSetter(setter);
    }

    /**
     * Returns a string representation of the map property descriptor.
     *
     * @return a formatted string representing the key and value types
     */
    @Override
    public String toString() {
        return "[%s] : %s -> %s".formatted(getName(), getKeyType().getJavaType(), getValueType().getJavaType());
    }
}
