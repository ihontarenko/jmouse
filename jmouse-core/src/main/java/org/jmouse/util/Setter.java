package org.jmouse.util;

import java.lang.reflect.Method;
import java.util.Map;

import static org.jmouse.core.reflection.Reflections.getMethodName;

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

    /**
     * Exception thrown when a setter method invocation fails.
     * <p>
     * This exception wraps the original cause of the failure, providing additional context.
     * </p>
     */
    class SetterCallException extends RuntimeException {

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
}
