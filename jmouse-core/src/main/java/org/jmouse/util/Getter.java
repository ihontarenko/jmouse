package org.jmouse.util;

import java.lang.reflect.Method;
import java.util.Map;

import static org.jmouse.core.reflection.Reflections.getMethodName;

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

    /**
     * Exception thrown when a getter method invocation fails.
     * <p>
     * This exception wraps the original cause of the failure, providing additional context.
     * </p>
     */
    class GetterCallException extends RuntimeException {

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
}
