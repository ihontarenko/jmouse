package org.jmouse.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * A utility for managing value-based transformations and conditional execution.
 * <p>
 * {@link AttributeMapper} provides a fluent API for defining transformations, conditions,
 * and executions based on a wrapped value provided via a {@link Supplier}.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Example of numerical value validation with AND and OR conditions
 * AttributeMapper.get()
 *     .get(() -> 15)
 *     .when(x -> x > 10)
 *     .and(x -> x < 20) // Executes if x > 10 and x < 20
 *     .accept(value -> System.out.println("Valid number: " + value));
 *
 * // Example of string validation with an OR condition
 * AttributeMapper.get()
 *     .get(() -> "Hello")
 *     .when(String::isEmpty)
 *     .when(s -> s.startsWith("H"), false) // Equivalent to OR condition
 *     .accept(value -> System.out.println("String matches condition: " + value));
 *
 * // Transforming a string to uppercase if it is not null
 * AttributeMapper.get()
 *     .get(() -> "java")
 *     .whenNonNull()
 *     .as(String::toUpperCase)
 *     .accept(value -> System.out.println("Uppercased: " + value));
 *
 * // Converting a numerical value to an integer if it is greater than 0
 * AttributeMapper.get()
 *     .get(() -> 42.8)
 *     .when(x -> x > 0)
 *     .asInt(Number::intValue)
 *     .accept(value -> System.out.println("Converted to int: " + value));
 * }</pre>
 */

public class AttributeMapper {

    private static final AttributeMapper MAPPER = new AttributeMapper();

    private AttributeMapper() {
    }

    /**
     * Retrieves the singleton instance of {@link AttributeMapper}.
     *
     * @return the singleton instance
     */
    public static AttributeMapper get() {
        return MAPPER;
    }

    /**
     * Creates a new {@link Container} instance based on a given {@link Supplier}.
     *
     * @param supplier the value supplier
     * @param <T>      the type of the value
     * @return a new {@link Container} instance
     */
    public <T> Container<T> get(Supplier<T> supplier) {
        return new Container<>(SingletonSupplier.of(supplier), null);
    }

    /**
     * Creates a new {@link Container} by accessing a value from the given {@link Map} using a specified key.
     *
     * <p>The map is not copied, so the resulting {@link Container} reflects the current value associated with the key
     * when {@code Value#get(Map, K)} is called.
     *
     * @param map the source map
     * @param key the key whose value should be retrieved
     * @param <K> the key type
     * @param <T> the value type
     * @return a {@link Container} representing the value at the specified key
     */
    public <K, T> Container<T> get(Map<K, T> map, K key) {
        return new Container<>(SingletonSupplier.of(() -> map.get(key)), null);
    }

    /**
     * Creates a new {@link Container} by accessing an element from a {@link List} at a specified index.
     *
     * <p>The list is not copied, so the resulting {@link Container} retrieves the element at the given index
     * at the time {@code Value#get(List, int)} is invoked.
     *
     * @param list  the source list
     * @param index the index of the desired element
     * @param <T>   the type of the list elements
     * @return a {@link Container} wrapping the list element at the given index
     * @throws IndexOutOfBoundsException if the index is invalid at the time of access
     */
    public <T> Container<T> get(List<T> list, int index) {
        return new Container<>(SingletonSupplier.of(() -> list.get(index)), null);
    }

    /**
     * Creates a new {@link Container} instance from a given structured.
     * This is a convenient method that wraps the provided instance into a supplier.
     *
     * @param instance the instance to wrap
     * @param <T>      the type of the instance
     * @return a {@link Container} wrapping the given instance
     */
    public <T> Container<T> get(T instance) {
        return get(() -> instance);
    }

    /**
     * Functional interface representing a void function.
     */
    @FunctionalInterface
    public interface VoidFunction {
        void call();
    }

    /**
     * A container for a value that supports transformations, conditions, and actions.
     *
     * @param <T> the type of the contained value
     */
    final public static class Container<T> {

        private final Supplier<T>  value;
        private final Predicate<T> predicate;

        private Container(final Supplier<T> value, final Predicate<T> predicate) {
            this.value = value;
            this.predicate = predicate;
        }

        /**
         * Converts the value to another type using the given function.
         *
         * @param adapter the transformation function
         * @param <R>     the target type
         * @return a new {@link Container} instance with the transformed type
         */
        public <R> Container<R> as(final Function<T, R> adapter) {
            Supplier<Boolean> external  = () -> predicate.test(value.get());
            Supplier<R>       supplier  = () -> external.get() ? adapter.apply(value.get()) : null;
            Predicate<R>      predicate = (v) -> external.get();
            return new Container<>(supplier, predicate);
        }

        /**
         * Converts the value to an integer using a function that extracts a {@link Number}.
         *
         * @param adapter the transformation function
         * @param <R>     the number type
         * @return a new {@link Container} containing an integer
         */
        public <R extends Number> Container<Integer> asInt(final Function<T, R> adapter) {
            return as(adapter).as(Number::intValue);
        }

        /**
         * Adds a condition to the value.
         *
         * @param predicate the predicate to apply
         * @return a new {@link Container} instance with the applied condition
         */
        public Container<T> when(final Predicate<T> predicate, final boolean condition) {
            Predicate<T> self = this.predicate;
            self = (self == null) ? predicate : (condition ? predicate.and(self) : predicate.or(self));
            return new Container<>(value, self);
        }

        /**
         * Adds a condition to the value.
         *
         * @param predicate the predicate to apply
         * @return a new {@link Container} instance with the applied condition
         */
        public Container<T> when(final Predicate<T> predicate) {
            return when(predicate, true);
        }

        /**
         * Combines the current condition with another using logical AND.
         *
         * @param predicate the additional predicate
         * @return a new {@link Container} instance with the combined condition
         */
        public Container<T> and(final Predicate<T> predicate) {
            return when(predicate, true);
        }

        /**
         * Combines the current condition with another using logical OR.
         *
         * @param predicate the additional predicate
         * @return a new {@link Container} instance with the combined condition
         */
        public Container<T> or(final Predicate<T> predicate) {
            return when(predicate, false);
        }

        /**
         * Ensures the value is non-null before proceeding.
         *
         * @return a new {@link Container} instance with a null check
         */
        public Container<T> whenNonNull() {
            return new Container<>(new NullSafeSupplier<>(value), Objects::nonNull);
        }

        /**
         * Inverts the existing condition.
         *
         * @return a new {@link Container} instance with a negated condition
         */
        public Container<T> whenNot() {
            return when(predicate.negate());
        }

        /**
         * Applies an action if the value is {@code false}.
         *
         * @return a new {@link Container} instance with the applied condition
         */
        public Container<T> whenFalse() {
            return when(FALSE::equals);
        }

        /**
         * Applies an action if the value is {@code true}.
         *
         * @return a new {@link Container} instance with the applied condition
         */
        public Container<T> whenTrue() {
            return when(TRUE::equals);
        }

        /**
         * Executes a consumer if the value satisfies the current condition.
         *
         * @param consumer the consumer to execute
         */
        public void accept(final Consumer<T> consumer) {
            if (test(value.get())) {
                consumer.accept(value.get());
            }
        }

        /**
         * Transforms the value if the condition is met.
         *
         * @param function the transformation function
         * @param <R>      the resulting type
         * @return the transformed result, or {@code null} if the condition is not met
         */
        public <R> R toInstance(Function<T, R> function) {
            return predicate.test(value.get()) ? function.apply(value.get()) : null;
        }

        /**
         * Executes a function if the value satisfies the current condition.
         *
         * @param function the function to execute
         */
        public void call(final VoidFunction function) {
            if (predicate.test(value.get())) {
                function.call();
            }
        }

        private boolean test(T value) {
            return Objects.requireNonNullElseGet(predicate, () -> (T v) -> true).test(value);
        }

    }

}
