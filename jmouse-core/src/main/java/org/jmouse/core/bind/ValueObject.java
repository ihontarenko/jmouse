package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.bind.descriptor.structured.record.ValueObjectIntrospector;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.core.CachedSupplier;
import org.jmouse.core.Factory;
import org.jmouse.util.Arrays;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;

import static org.jmouse.core.reflection.InferredType.forClass;
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
    public Factory<T> getInstance(Values values) {
        if (values.owner != this) {
            throw new IllegalArgumentException("Virtual instances can only be owned by " + this);
        }

        return Factory.of(new CachedSupplier<>(() -> {
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
                Object       value        = values.get(property);
                InferredType propertyType = property.getType().getJavaType();
                Class<?>     classType    = Arrays.boxType(propertyType.getRawType());

                try {
                    arguments[index++] = classType.cast(value);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(
                            "Property '%s' of type '%s' cannot be cast to '%s'. Value should be converted or passed before"
                                    .formatted(property.getName(), propertyType, value.getClass()));
                }
            }

            return Reflections.instantiate(constructor, arguments);
        }));
    }

    /**
     * @return a canonical {@code Constructor} for this record
     */
    public Constructor<T> getCanonicalConstructor() {
        return constructor;
    }

    @Override
    public String toString() {
        return "ValueObject: %s; Properties: %d".formatted(type, getProperties().size());
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
