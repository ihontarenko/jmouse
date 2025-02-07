package org.jmouse.core.bind;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Arrays;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.jmouse.core.reflection.JavaType.forClass;
import static org.jmouse.core.reflection.Reflections.findFirstConstructor;
import static org.jmouse.core.reflection.Reflections.getRecordComponentsTypes;

/**
 * Represents a wrapper for Java records, allowing for structured value retrieval and instantiation.
 * <p>
 * This class provides mechanisms to introspect record components, store their values, and create instances.
 * </p>
 *
 * @param <T> the type of the record
 */
final public class ValueObject<T extends Record> extends Bean<T> {

    private Constructor<T> constructor;

    private ValueObject(Class<?> type) {
        super(forClass(type));
        addComponents();
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
     * Extracts record components and initializes properties for the record type.
     */
    private void addComponents() {
        Class<?> recordType = type.getRawType();

        if (recordType.isRecord()) {
            Stream.of(recordType.getRecordComponents()).forEach(this::addProperty);
        }

        constructor = (Constructor<T>) findFirstConstructor(recordType, getRecordComponentsTypes(recordType));
    }

    /**
     * Adds a record component as a property.
     *
     * @param component the record component to be added
     */
    private void addProperty(RecordComponent component) {
        if (component != null) {
            String      name     = component.getName();
            Property<T> property = createProperty(name);
            property.setComponent(component);
            property.setRawGetter(component.getAccessor());
            properties.put(name, property);
        }
    }

    /**
     * Creates a new property for the record component.
     *
     * @param name the property name
     * @return a new {@code Property} instance
     */
    private Property<T> createProperty(String name) {
        return new Property<>(name, this.type);
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
            Object[] arguments = new Object[properties.size()];
            int      index     = 0;

            if (constructor == null) {
                throw new IllegalStateException(
                        "No applicable constructor was found for this record type: %s".formatted(type));
            }

            if (values.size() != properties.size()) {
                throw new IllegalArgumentException(
                        "ValueObject(%s) should be fully fulfilled with '%d' properties but only '%d' present"
                                .formatted(type, properties.size(), values.size()));
            }

            for (Bean.Property<?> property : getProperties()) {
                Object   value        = values.get(property);
                JavaType propertyType = property.getType();
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
        return "ValueObject: %s; Properties: %d".formatted(type, properties.size());
    }

    /**
     * Represents a record component property within a {@code ValueObject}.
     *
     * @param <T> the type of the record
     */
    public static class Property<T> extends Bean.Property<T> {

        private RecordComponent component;

        public Property(String name, JavaType owner) {
            super(name, owner);
        }

        @Override
        public <V> void setValue(Factory<T> factory, V value) {
            throw new UnsupportedOperationException("Record is immutable and unable to set value");
        }

        /**
         * Gets the associated record component.
         *
         * @return the record component
         */
        public RecordComponent getComponent() {
            return component;
        }

        /**
         * Sets the associated record component.
         *
         * @param component the record component to set
         */
        public void setComponent(RecordComponent component) {
            this.component = component;
        }
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
        public Object get(Bean.Property<?> property) {
            return get(property.getName());
        }
    }

}
