package org.jmouse.core.access.descriptor.structured.map;

import org.jmouse.core.access.descriptor.ClassTypeDescriptor;
import org.jmouse.core.access.descriptor.structured.PropertyData;

import java.util.Map;

/**
 * Represents structured property data for a {@link Map} entry.
 * <p>
 * This class extends {@link PropertyData} and includes additional metadata for
 * the key and value types of the map property.
 * </p>
 *
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 *
 * @author JMouse - Team
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public class MapPropertyData<K, V> extends PropertyData<Map<K, V>> {

    private ClassTypeDescriptor keyType;
    private ClassTypeDescriptor valueType;

    /**
     * Constructs a {@code MapPropertyData} instance for the given map.
     *
     * @param target the map to analyze
     */
    public MapPropertyData(Map<K, V> target) {
        super(target);
    }

    /**
     * Returns the type descriptor of the map keys.
     *
     * @return a {@link ClassTypeDescriptor} representing the key type
     */
    public ClassTypeDescriptor getKeyType() {
        return keyType;
    }

    /**
     * Sets the type descriptor of the map keys.
     *
     * @param keyType the {@link ClassTypeDescriptor} representing the key type
     */
    public void setKeyType(ClassTypeDescriptor keyType) {
        this.keyType = keyType;
    }

    /**
     * Returns the type descriptor of the map values.
     *
     * @return a {@link ClassTypeDescriptor} representing the value type
     */
    public ClassTypeDescriptor getValueType() {
        return valueType;
    }

    /**
     * Sets the type descriptor of the map values.
     *
     * @param valueType the {@link ClassTypeDescriptor} representing the value type
     */
    public void setValueType(ClassTypeDescriptor valueType) {
        this.valueType = valueType;
    }
}
