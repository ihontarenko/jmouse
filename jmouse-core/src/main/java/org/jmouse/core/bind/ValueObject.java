package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanIntrospector;
import org.jmouse.core.bind.descriptor.structured.vo.ValueObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.vo.ValueObjectIntrospector;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.CachedSupplier;
import org.jmouse.util.Factory;
import org.jmouse.util.SingletonSupplier;
import org.jmouse.util.helper.Arrays;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.jmouse.core.reflection.JavaType.forClass;
import static org.jmouse.core.reflection.Reflections.*;

/**
 * Represents a wrapper for Java records, allowing for structured value retrieval and instantiation.
 * <p>
 * This class provides mechanisms to introspect record components, store their values, and create instances.
 * </p>
 *
 * @param <T> the type of the record
 */
final public class ValueObject<T extends Record> extends Bean<T> {

    private final Constructor<T> constructor;

    @SuppressWarnings({"unchecked"})
    private ValueObject(Class<?> type) {
        super(forClass(type), new ValueObjectIntrospector<>((Class<T>) type).introspect().toDescriptor());
        constructor = (Constructor<T>) findFirstConstructor(type, getRecordComponentsTypes(type));
    }

    /**
     * Creates a {@code ValueObject} for the given record type.
     *
     * @param type the record class
     * @param <I>  the type of the record
     * @return a new {@code ValueObject} instance
     */
    public static <I extends Record> ValueObject<I> of(Class<I> type) {
        return new ValueObject<>(type);
    }

    /**
     * Returns a new instance of {@code Values}, which holds key-value pairs for record components.
     *
     * @return an empty {@code Values} instance
     */
    public Values getRecordValues() {
        return new Values(this);
    }

    /**
     * Creates a supplier that instantiates the record based on the provided values.
     *
     * @param values the values to populate the record
     * @return a supplier providing a new record instance
     * @throws IllegalArgumentException if the values do not match the expected properties
     * @throws IllegalStateException    if no constructor is found for the record type
     */
    public Supplier<T> getInstance(Values values) {
        if (values.owner != this) {
            throw new IllegalArgumentException("Virtual instances can only be owned by " + this);
        }

        return new CachedSupplier<>(() -> {
            Object[] arguments = new Object[getProperties().size()];
            int      index     = 0;

            if (constructor == null) {
                throw new IllegalStateException(
                        "No applicable constructor was found for this record type: %s".formatted(type));
            }

            if (values.size() != getProperties().size()) {
                throw new IllegalArgumentException(
                        "Property mismatch: %s requires all %d properties to be set, but found only %d"
                                .formatted(type, getProperties().size(), values.size()));
            }

            for (PropertyDescriptor<?> property : getProperties()) {
                Object   value        = values.get(property);
                JavaType propertyType = property.getType().getJavaType();
                Class<?> classType    = Arrays.boxType(propertyType.getRawType());

                try {
                    arguments[index++] = classType.cast(value);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(
                            "Property '%s' of type '%s' cannot be cast to '%s'. Value should be converted or passed before"
                                    .formatted(property.getName(), propertyType, value.getClass()));
                }
            }

            return Reflections.instantiate(constructor, arguments);
        });
    }

    @Override
    public String toString() {
        return "ValueObject: %s; Properties: %d".formatted(type, getProperties().size());
    }

    /**
     * Retrieves all properties defined in this structured.
     *
     * @return a collection of properties
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public Collection<? extends PropertyDescriptor<T>> getProperties() {
        return ((ValueObjectDescriptor<T>)descriptor).getProperties().values();
    }

    /**
     * Retrieves a specific property by name.
     *
     * @param name the name of the property
     * @return the property associated with the given name, or {@code null} if not found
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public PropertyDescriptor<T> getProperty(String name) {
        return ((ValueObjectDescriptor<T>)descriptor).getProperty(name);
    }

    /**
     * Checks whether this structured contains a property with the given name.
     *
     * @param name the property name to check
     * @return {@code true} if the property exists, otherwise {@code false}
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public boolean hasProperty(String name) {
        return ((ValueObjectDescriptor<T>)descriptor).hasProperty(name);
    }

    /**
     * A container for record component values, ensuring consistency with the owning {@code ValueObject}.
     */
    public static final class Values extends LinkedHashMap<String, Object> {

        private final ValueObject<?> owner;

        public Values(ValueObject<?> owner) {
            this.owner = owner;
        }

        @Override
        public Object put(String key, Object value) {
            Object previous = null;

            if (owner.hasProperty(key)) {
                previous = super.put(key, value);
            }

            return previous;
        }

        /**
         * Sets a value for the specified key if it exists in the {@code ValueObject}.
         *
         * @param key   the property key
         * @param value the value to assign
         */
        public void set(String key, Object value) {
            if (owner.hasProperty(key)) {
                put(key, value);
            }
        }

        /**
         * Retrieves the value associated with the given key.
         *
         * @param key the property key
         * @return the stored value or {@code null} if not present
         */
        public Object get(String key) {
            return owner.hasProperty(key) ? super.get(key) : null;
        }

        /**
         * Retrieves the value associated with the given property.
         *
         * @param property the property reference
         * @return the stored value or {@code null} if not present
         */
        public Object get(PropertyDescriptor<?> property) {
            return get(property.getName());
        }
    }

}
