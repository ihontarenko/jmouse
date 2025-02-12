package org.jmouse.util;

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
 * {@link ValueFlow} provides a fluent API for defining transformations, conditions,
 * and executions based on a wrapped value provided via a {@link Supplier}.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Example of numerical value validation with AND and OR conditions
 * ValueFlow.get()
 *     .create(() -> 15)
 *     .when(x -> x > 10)
 *     .and(x -> x < 20) // Executes if x > 10 and x < 20
 *     .toConsume(value -> System.out.println("Valid number: " + value));
 *
 * // Example of string validation with an OR condition
 * ValueFlow.get()
 *     .create(() -> "Hello")
 *     .when(String::isEmpty)
 *     .when(s -> s.startsWith("H"), false) // Equivalent to OR condition
 *     .toConsume(value -> System.out.println("String matches condition: " + value));
 *
 * // Transforming a string to uppercase if it is not null
 * ValueFlow.get()
 *     .create(() -> "java")
 *     .whenNonNull()
 *     .as(String::toUpperCase)
 *     .toConsume(value -> System.out.println("Uppercased: " + value));
 *
 * // Converting a numerical value to an integer if it is greater than 0
 * ValueFlow.get()
 *     .create(() -> 42.8)
 *     .when(x -> x > 0)
 *     .asInt(Number::intValue)
 *     .toConsume(value -> System.out.println("Converted to int: " + value));
 * }</pre>
 */

public class ValueFlow {

    private static final ValueFlow MAPPER = new ValueFlow();

    private ValueFlow() {
    }

    /**
     * Retrieves the singleton instance of {@link ValueFlow}.
     *
     * @return the singleton instance
     */
    public static ValueFlow get() {
        return MAPPER;
    }

    /**
     * Creates a new {@link Value} instance based on a given {@link Supplier}.
     *
     * @param supplier the value supplier
     * @param <T>      the type of the value
     * @return a new {@link Value} instance
     */
    public <T> Value<T> create(Supplier<T> supplier) {
        return new Value<>(SingletonSupplier.of(supplier), null);
    }

    /**
     * Creates a new {@link ValueFlow.Value} instance from a given bean.
     * This is a convenient method that wraps the provided instance into a supplier.
     *
     * @param instance the instance to wrap
     * @param <T>      the type of the instance
     * @return a {@link ValueFlow.Value} wrapping the given instance
     */
    public <T> Value<T> create(T instance) {
        return create(() -> instance);
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
    final public static class Value<T> {

        private final Supplier<T>  value;
        private final Predicate<T> predicate;

        private Value(final Supplier<T> value, final Predicate<T> predicate) {
            this.value = value;
            this.predicate = predicate;
        }

        /**
         * Converts the value to another type using the given function.
         *
         * @param adapter the transformation function
         * @param <R>     the target type
         * @return a new {@link Value} instance with the transformed type
         */
        public <R> Value<R> as(final Function<T, R> adapter) {
            Supplier<Boolean> external  = () -> predicate.test(value.get());
            Supplier<R>       supplier  = () -> external.get() ? adapter.apply(value.get()) : null;
            Predicate<R>      predicate = (v) -> external.get();
            return new Value<>(supplier, predicate);
        }

        /**
         * Converts the value to an integer using a function that extracts a {@link Number}.
         *
         * @param adapter the transformation function
         * @param <R>     the number type
         * @return a new {@link Value} containing an integer
         */
        public <R extends Number> Value<Integer> asInt(final Function<T, R> adapter) {
            return as(adapter).as(Number::intValue);
        }

        /**
         * Adds a condition to the value.
         *
         * @param predicate the predicate to apply
         * @return a new {@link Value} instance with the applied condition
         */
        public Value<T> when(final Predicate<T> predicate, final boolean condition) {
            Predicate<T> self = this.predicate;
            self = self == null ? predicate : condition ? predicate.and(self) : predicate.or(self);
            return new Value<>(value, self);
        }

        /**
         * Adds a condition to the value.
         *
         * @param predicate the predicate to apply
         * @return a new {@link Value} instance with the applied condition
         */
        public Value<T> when(final Predicate<T> predicate) {
            return when(predicate, true);
        }

        /**
         * Combines the current condition with another using logical AND.
         *
         * @param predicate the additional predicate
         * @return a new {@link Value} instance with the combined condition
         */
        public Value<T> and(final Predicate<T> predicate) {
            return when(predicate, true);
        }

        /**
         * Combines the current condition with another using logical OR.
         *
         * @param predicate the additional predicate
         * @return a new {@link Value} instance with the combined condition
         */
        public Value<T> or(final Predicate<T> predicate) {
            return when(predicate, false);
        }

        /**
         * Ensures the value is non-null before proceeding.
         *
         * @return a new {@link Value} instance with a null check
         */
        public Value<T> whenNonNull() {
            return new Value<>(new NullSafeSupplier<>(value), Objects::nonNull);
        }

        /**
         * Inverts the existing condition.
         *
         * @return a new {@link Value} instance with a negated condition
         */
        public Value<T> whenNot() {
            return when(predicate.negate());
        }

        /**
         * Applies an action if the value is {@code false}.
         *
         * @return a new {@link Value} instance with the applied condition
         */
        public Value<T> whenFalse() {
            return when(FALSE::equals);
        }

        /**
         * Applies an action if the value is {@code true}.
         *
         * @return a new {@link Value} instance with the applied condition
         */
        public Value<T> whenTrue() {
            return when(TRUE::equals);
        }

        /**
         * Executes a consumer if the value satisfies the current condition.
         *
         * @param consumer the consumer to execute
         */
        public void toConsume(final Consumer<T> consumer) {
            if (predicate.test(value.get())) {
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
        public void toCall(final VoidFunction function) {
            if (predicate.test(value.get())) {
                function.call();
            }
        }

    }

}
