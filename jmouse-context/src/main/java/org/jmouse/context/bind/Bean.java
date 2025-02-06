package org.jmouse.context.bind;

import org.jmouse.core.reflection.JavaType;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.jmouse.core.reflection.Reflections.getMethodName;

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
     * A functional interface representing a getter method for retrieving a value from an instance.
     * <p>
     * This interface abstracts over different ways of accessing properties, such as via method calls
     * or retrieving values from a map.
     * </p>
     *
     * @param <T> the type of the instance from which the value is retrieved
     * @param <R> the type of the value being retrieved
     */
    @FunctionalInterface
    public interface Getter<T, R> {

        /**
         * Creates a getter for a map, retrieving a value by a given key.
         *
         * @param key the key whose associated value is to be returned
         * @param <K> the type of keys in the map
         * @param <V> the type of values in the map
         * @return a getter that retrieves values from a map by key
         */
        static <K, V> Getter<Map<K, V>, V> ofMap(K key) {
            return (instance) -> instance.get(key);
        }

        /**
         * Creates a getter that invokes a given method on an instance.
         *
         * @param getter the method to be invoked as a getter
         * @param <T>    the type of the instance
         * @param <V>    the type of the value being retrieved
         * @return a getter that calls the specified method
         * @throws GetterCallException if the method invocation fails
         */
        @SuppressWarnings({"unchecked"})
        static <T, V> Getter<T, V> ofMethod(Method getter) {
            return (T instance) -> {
                try {
                    getter.setAccessible(true);
                    return (V) getter.invoke(instance);
                } catch (Exception exception) {
                    throw new GetterCallException(
                            "Failed to call getter '%s'".formatted(getMethodName(getter)), exception);
                }
            };
        }

        /**
         * Retrieves a value from the given instance.
         *
         * @param instance the instance from which to retrieve the value
         * @return the retrieved value
         */
        R get(T instance);
    }

    /**
     * A functional interface representing a setter method for assigning a value to an instance.
     * <p>
     * This interface abstracts over different ways of setting properties, such as via method calls
     * or assigning values in a map.
     * </p>
     *
     * @param <T> the type of the instance on which the value is set
     * @param <V> the type of the value being set
     */
    @FunctionalInterface
    public interface Setter<T, V> {

        /**
         * Creates a setter for a map, assigning a value to a specified key.
         *
         * @param key the key to which the value should be assigned
         * @param <K> the type of keys in the map
         * @param <V> the type of values in the map
         * @return a setter that assigns values in a map by key
         */
        static <K, V> Setter<Map<K, V>, V> ofMap(K key) {
            return (instance, value) -> instance.put(key, value);
        }

        /**
         * Creates a setter that invokes a given method on an instance.
         *
         * @param setter the method to be invoked as a setter
         * @param <T>    the type of the instance
         * @param <V>    the type of the value being set
         * @return a setter that calls the specified method
         * @throws SetterCallException if the method invocation fails
         */
        static <T, V> Setter<T, V> ofMethod(Method setter) {
            return (T instance, V value) -> {
                try {
                    setter.setAccessible(true);
                    setter.invoke(instance, value);
                } catch (Exception exception) {
                    throw new SetterCallException(
                            "Failed to call setter '%s'".formatted(getMethodName(setter)), exception);
                }
            };
        }

        /**
         * Assigns a value to the given instance.
         *
         * @param instance the instance on which to set the value
         * @param value    the value to be assigned
         */
        void set(T instance, V value);

    }

    /**
     * A functional interface representing a factory for creating instances of type {@code T}.
     * <p>
     * This interface allows encapsulating object creation logic, making it useful in contexts
     * where dependency injection, lazy initialization, or configurable instantiation is required.
     * </p>
     *
     * @param <T> the type of object this factory creates
     */
    @FunctionalInterface
    public interface Factory<T> {

        /**
         * Creates a factory from a given supplier.
         *
         * @param supplier the supplier providing instances of {@code T}
         * @param <T>      the type of the created object
         * @return a factory that delegates to the supplier
         */
        static <T> Factory<T> of(Supplier<T> supplier) {
            return supplier::get;
        }

        /**
         * Creates and returns a new instance of {@code T}.
         *
         * @return a new instance of {@code T}
         */
        T create();
    }

    /**
     * Exception thrown when a setter method invocation fails.
     * <p>
     * This exception wraps the original cause of the failure, providing additional context.
     * </p>
     */
    public static class SetterCallException extends RuntimeException {

        /**
         * Constructs a new {@code SetterCallException} with the specified message and cause.
         *
         * @param message the detail message
         * @param cause   the underlying cause of the exception
         */
        public SetterCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when a getter method invocation fails.
     * <p>
     * This exception wraps the original cause of the failure, providing additional context.
     * </p>
     */
    public static class GetterCallException extends RuntimeException {

        /**
         * Constructs a new {@code GetterCallException} with the specified message and cause.
         *
         * @param message the detail message
         * @param cause   the underlying cause of the exception
         */
        public GetterCallException(String message, Throwable cause) {
            super(message, cause);
        }
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
        protected       Method            rawSetter;
        protected       Getter<T, Object> getter;
        protected       Setter<T, Object> setter;

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
            JavaType type = JavaType.forMethodReturnType(rawGetter);

            // Prefer the setter type if available
            if (rawSetter != null) {
                type = JavaType.forMethodParameter(rawSetter, 0);
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
            return "(%s) [%s] : %s".formatted(owner, name, getType());
        }
    }


}
