package org.jmouse.core.bind;

import org.jmouse.core.reflection.JavaType;
import org.jmouse.util.Factory;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Represents a generic bean model that encapsulates metadata and properties of a Java type.
 * <p>
 * This class provides methods for managing properties dynamically, enabling reflection-based
 * property access and manipulation.
 * </p>
 *
 * @param <T> the type of the bean instance
 */
abstract public class Bean<T> {

    /**
     * The Java type represented by this bean.
     */
    protected final JavaType type;

    /**
     * A map storing the properties of this bean, keyed by property names.
     */
    protected final Map<String, Property<T>> properties = new LinkedHashMap<>();

    /**
     * Constructs a bean for the specified type.
     *
     * @param type the Java type this bean represents
     */
    protected Bean(JavaType type) {
        this.type = type;
    }

    /**
     * Retrieves all properties defined in this bean.
     *
     * @return a collection of properties
     */
    public Collection<? extends Property<T>> getProperties() {
        return properties.values();
    }

    /**
     * Retrieves a specific property by name.
     *
     * @param name the name of the property
     * @return the property associated with the given name, or {@code null} if not found
     */
    public Property<T> getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Checks whether this bean contains a property with the given name.
     *
     * @param name the property name to check
     * @return {@code true} if the property exists, otherwise {@code false}
     */
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }


    /**
     * Represents a property within a bean or record, encapsulating getter and setter methods.
     * <p>
     * This abstract class provides a mechanism for retrieving and modifying property values dynamically
     * using reflection. It supports both read and write operations if applicable.
     * </p>
     *
     * @param <T> the type of the bean or record containing this property
     */
    public abstract static class Property<T> {

        protected final String            name;
        protected final JavaType          owner;
        protected       Method            rawGetter;
        protected Method            rawSetter;
        protected Getter<T, Object> getter;
        protected Setter<T, Object> setter;

        /**
         * Constructs a property with the specified name and owner type.
         *
         * @param name  the name of the property
         * @param owner the owning class type
         */
        public Property(String name, JavaType owner) {
            this.name = name;
            this.owner = owner;
        }

        /**
         * Gets the property name.
         *
         * @return the property name
         */
        public String getName() {
            return name;
        }

        /**
         * Retrieves the value of this property from an instance provided by the factory.
         *
         * @param factory a factory supplying an instance of {@code T}
         * @return a supplier returning the property value
         */
        public Supplier<Object> getValue(Factory<T> factory) {
            return () -> getGetter().get(factory.create());
        }

        /**
         * Sets the value of this property on an instance provided by the factory.
         *
         * @param factory a factory supplying an instance of {@code T}
         * @param value   the value to set
         * @param <V>     the type of the value
         */
        public <V> void setValue(Factory<T> factory, V value) {
            getSetter().set(factory.create(), value);
        }

        /**
         * Gets the setter method for this property.
         *
         * @return the setter method
         */
        public Setter<T, Object> getSetter() {
            return setter;
        }

        /**
         * Sets the setter method for this property.
         *
         * @param setter the setter method
         */
        public void setSetter(Setter<T, Object> setter) {
            this.setter = setter;
        }

        /**
         * Gets the getter method for this property.
         *
         * @return the getter method
         */
        public Getter<T, Object> getGetter() {
            return getter;
        }

        /**
         * Sets the getter method for this property.
         *
         * @param getter the getter method
         */
        public void setGetter(Getter<T, Object> getter) {
            this.getter = getter;
        }

        /**
         * Determines the property type based on its getter or setter method.
         *
         * @return the JavaType representing the property's type
         */
        public JavaType getType() {
            JavaType type = JavaType.NONE_TYPE;

            // Prefer the setter type if available and getter as fallback type
            if (rawSetter != null) {
                type = JavaType.forMethodParameter(rawSetter, 0);
            } else if (rawGetter != null) {
                type = JavaType.forMethodReturnType(rawGetter);
            }

            return type;
        }

        /**
         * Gets the raw getter method used for property access.
         *
         * @return the raw getter method
         */
        public Method getRawGetter() {
            return rawGetter;
        }

        /**
         * Sets the raw getter method and initializes the corresponding getter.
         *
         * @param rawGetter the raw getter method
         */
        public void setRawGetter(Method rawGetter) {
            this.rawGetter = rawGetter;
            if (rawGetter != null) {
                setGetter(Getter.ofMethod(rawGetter));
            }
        }

        /**
         * Gets the raw setter method used for property modification.
         *
         * @return the raw setter method
         */
        public Method getRawSetter() {
            return rawSetter;
        }

        /**
         * Sets the raw setter method and initializes the corresponding setter.
         *
         * @param rawSetter the raw setter method
         */
        public void setRawSetter(Method rawSetter) {
            this.rawSetter = rawSetter;
            if (rawSetter != null) {
                setSetter(Setter.ofMethod(rawSetter));
            }
        }

        /**
         * Checks if this property supports writing (i.e., has a setter).
         *
         * @return {@code true} if writable, otherwise {@code false}
         */
        public boolean isWritable() {
            return rawSetter != null;
        }

        /**
         * Checks if this property supports reading (i.e., has a getter).
         *
         * @return {@code true} if readable, otherwise {@code false}
         */
        public boolean isReadable() {
            return rawGetter != null;
        }

        @Override
        public String toString() {
            return "%s#%s : %s".formatted(owner, name, getType());
        }
    }


}
