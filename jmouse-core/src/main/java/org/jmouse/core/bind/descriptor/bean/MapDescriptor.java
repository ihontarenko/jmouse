package org.jmouse.core.bind.descriptor.bean;

import org.jmouse.core.bind.descriptor.AnnotationDescriptor;
import org.jmouse.core.bind.descriptor.TypeDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.jmouse.core.bind.descriptor.TypeDescriptor.forType;

/**
 * Represents a descriptor for a {@link Map}, providing descriptor about its structure and entries.
 * <p>
 * This interface extends {@link ObjectDescriptor}, allowing introspection of map properties,
 * including its keys and values.
 * </p>
 *
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface MapDescriptor<K, V> extends ObjectDescriptor<Map<K, V>> {

    /**
     * Creates a {@link MapDescriptor} instance for the given {@link Map}.
     * <p>
     * This method builds a descriptor that represents the given map, including its
     * structure and individual entries as properties.
     * </p>
     *
     * @param map the map to describe
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @return a new {@link MapDescriptor} instance representing the given map
     */
    @SuppressWarnings("unchecked")
    static <K, V> MapDescriptor<K, V> forMap(final Map<K, V> map) {
        Builder<K, V> root = new Builder<>(null);

        root.target(map);
        root.descriptor(TypeDescriptor.forClass(map.getClass()));

        MapDescriptor<K, V> descriptor = root.toImmutable();

        for (Map.Entry<K, V> entry : map.entrySet()) {
            V value = entry.getValue();
            K key   = entry.getKey();

            // create new property builder
            MapPropertyDescriptor.Builder<K, V> builder = new MapPropertyDescriptor.Builder<>(key.toString());

            // Define getter and setter for the map entry
            Getter<Map<K, V>, V> getter = Getter.ofMap(key);
            Setter<Map<K, V>, V> setter = Setter.ofMap(key);

            builder.getter((Getter<Map<K, V>, Object>) getter);
            builder.setter((Setter<Map<K, V>, Object>) setter);

            // Define key and value types using TypeDescriptor
            builder.valueType(TypeDescriptor.forClass(value.getClass()));
            builder.keyType(TypeDescriptor.forClass(key.getClass()));

            builder.owner(descriptor);

            root.property(builder.toImmutable());
        }

        return descriptor;
    }

    /**
     * A default implementation of {@link MapDescriptor}.
     * <p>
     * This implementation extends {@link ObjectDescriptor.Implementation} and
     * provides descriptor and property introspection capabilities for {@link Map} instances.
     * </p>
     *
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     */
    class Implementation<K, V> extends ObjectDescriptor.Implementation<Map<K, V>> implements MapDescriptor<K, V> {

        /**
         * Constructs a new {@code BeanDescriptor.PropertyDescriptorAccessor} instance.
         *
         * @param name        the name of the bean
         * @param internal    the bean instance
         * @param annotations a set of annotation descriptors associated with this bean
         * @param type        the descriptor representing the class of this bean
         * @param properties  a map of property names to property descriptors
         */
        Implementation(
                String name,
                Map<K, V> internal,
                Set<AnnotationDescriptor> annotations,
                TypeDescriptor type,
                Map<String, PropertyDescriptor<Map<K, V>>> properties
        ) {
            super(name, internal, annotations, type, properties);
        }

        /**
         * Returns the class type being inspected.
         *
         * @return the {@link Class} bean representing the bean type
         */
        @Override
        public Class<?> getClassType() {
            return type.getClassType();
        }


        /**
         * Returns a string representation of this bean descriptor.
         *
         * @return a string representation of this bean descriptor
         */
        @Override
        public String toString() {
            return "[MP] : %s".formatted(super.toString());
        }
    }

    /**
     * A builder class for constructing {@link MapDescriptor} instances.
     * <p>
     * This builder extends {@link ObjectDescriptor.Builder} and provides methods
     * for configuring and constructing a {@link MapDescriptor} that represents
     * a {@link Map} with its associated properties and descriptor.
     * </p>
     *
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     */
    class Builder<K, V> extends ObjectDescriptor.Builder<Map<K, V>> {

        /**
         * Constructs a new {@code MapDescriptor.Mutable}.
         *
         * @param name the name of the descriptor being built
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Builds and returns a {@link MapDescriptor} instance.
         * <p>
         * The resulting descriptor is immutable, with its annotations and properties
         * wrapped in unmodifiable collections.
         * </p>
         *
         * @return a new instance of {@link MapDescriptor}
         */
        @Override
        public MapDescriptor<K, V> toImmutable() {
            return new Implementation<>(
                    name,
                    target,
                    Collections.unmodifiableSet(annotations),
                    descriptor,
                    Collections.unmodifiableMap(properties)
            );
        }
    }

}
