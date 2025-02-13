package org.jmouse.core.bind.descriptor.bean;

import org.jmouse.core.bind.descriptor.TypeDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a descriptor for a single entry in a {@link Map}, providing descriptor about
 * the key-value pair, its type, and access methods.
 * <p>
 * This interface extends {@link PropertyDescriptor} and allows introspection of
 * individual map entries as properties.
 * </p>
 *
 * @param <K> the type of the map key
 * @param <V> the type of the map value
 */
public interface MapPropertyDescriptor<K, V> extends PropertyDescriptor<Map<K, V>> {

    /**
     * Returns the descriptor representing the key type of this map entry.
     *
     * @return the {@link TypeDescriptor} for the key type
     */
    TypeDescriptor getKeyType();

    /**
     * Returns the descriptor representing the value type of this map entry.
     *
     * @return the {@link TypeDescriptor} for the value type
     */
    TypeDescriptor getValueType();

    /**
     * A default implementation of {@link MapPropertyDescriptor}.
     * <p>
     * This implementation extends {@link PropertyDescriptor.Implementation} and
     * provides additional descriptor about the key and value types of a map entry.
     * </p>
     *
     * @param <K> the type of the map key
     * @param <V> the type of the map value
     */
    class Implementation<K, V> extends PropertyDescriptor.Implementation<Map<K, V>> implements MapPropertyDescriptor<K, V> {

        private final TypeDescriptor keyType;
        private final TypeDescriptor valueType;

        /**
         * Constructs a new {@code MapPropertyDescriptor.PropertyDescriptorAccessor}.
         *
         * @param name        the name of the property
         * @param internal    the internal map reference
         * @param getter      the getter function for retrieving values from the map
         * @param setter      the setter function for modifying values in the map
         * @param owner       the owner descriptor representing the parent map
         * @param keyType     the descriptor representing the key type
         * @param valueType   the descriptor representing the value type
         */
        Implementation(
                String name,
                Map<K, V> internal,
                Getter<Map<K, V>, Object> getter,
                Setter<Map<K, V>, Object> setter,
                ObjectDescriptor<Map<K, V>> owner,
                TypeDescriptor keyType,
                TypeDescriptor valueType
        ) {
            super(name, internal, Collections.emptySet(), getter, setter, owner);

            this.keyType = keyType;
            this.valueType = valueType;
        }

        /**
         * Returns the class descriptor representing the type of this property.
         *
         * @return the {@link TypeDescriptor} representing the property's type
         */
        @Override
        public TypeDescriptor getType() {
            return getValueType();
        }

        /**
         * Returns the descriptor representing the key type of this map entry.
         *
         * @return the {@link TypeDescriptor} for the key type
         */
        @Override
        public TypeDescriptor getKeyType() {
            return keyType;
        }

        /**
         * Returns the descriptor representing the value type of this map entry.
         *
         * @return the {@link TypeDescriptor} for the value type
         */
        @Override
        public TypeDescriptor getValueType() {
            return valueType;
        }

        /**
         * Returns a string representation of this map property descriptor.
         *
         * @return a formatted string including the key and value types
         */
        @Override
        public String toString() {
            return "[%s : %s]".formatted(getName(), getValueType().getType());
        }
    }

    /**
     * A builder class for constructing {@link MapPropertyDescriptor} instances.
     *
     * @param <K> the type of the map key
     * @param <V> the type of the map value
     */
    class Builder<K, V> extends PropertyDescriptor.Builder<Builder<K, V>, Map<K, V>, MapPropertyDescriptor<K, V>> {

        private TypeDescriptor keyType;
        private TypeDescriptor valueType;

        /**
         * Constructs a new {@code MapPropertyDescriptor.Builder}.
         *
         * @param name the name of the map entry being built
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the key type descriptor for this map entry.
         *
         * @param keyType the {@link TypeDescriptor} representing the key type
         * @return this builder instance
         */
        public Builder<K, V> keyType(TypeDescriptor keyType) {
            this.keyType = keyType;
            return self();
        }

        /**
         * Sets the value type descriptor for this map entry.
         *
         * @param valueType the {@link TypeDescriptor} representing the value type
         * @return this builder instance
         */
        public Builder<K, V> valueType(TypeDescriptor valueType) {
            this.valueType = valueType;
            return self();
        }

        /**
         * Builds and returns a {@link MapPropertyDescriptor} instance.
         *
         * @return a new instance of {@link MapPropertyDescriptor}
         */
        @Override
        public MapPropertyDescriptor<K, V> build() {
            return new Implementation<>(
                    name,
                    internal,
                    getter,
                    setter,
                    owner,
                    keyType,
                    valueType
            );
        }
    }
}
