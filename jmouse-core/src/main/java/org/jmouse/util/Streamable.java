package org.jmouse.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a streamable iterable that provides convenient methods for
 * functional stream operations while maintaining iterable capabilities.
 *
 * @param <T> the type of elements contained in the streamable instance
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 */
@FunctionalInterface
public interface Streamable<T> extends Iterable<T>, Supplier<Stream<T>> {

    /**
     * Creates a sequential {@link Stream} from this iterable.
     *
     * @return a sequential stream of elements
     */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Creates a {@code Streamable} instance from an iterable.
     *
     * @param iterable the source iterable
     * @param <T>      the type of elements
     * @return a new {@code Streamable} instance
     */
    static <T> Streamable<T> of(Iterable<T> iterable) {
        return iterable::iterator;
    }

    /**
     * Creates a {@code Streamable} instance from an array of elements.
     *
     * @param elements the elements to include
     * @param <T>      the type of elements
     * @return a new {@code Streamable} instance
     */
    @SafeVarargs
    static <T> Streamable<T> of(T... elements) {
        return () -> List.of(elements).iterator();
    }

    /**
     * Creates a {@code Streamable} instance from a stream supplier.
     *
     * @param supplier the supplier providing a stream
     * @param <T>      the type of elements
     * @return a new {@code Streamable} instance
     */
    static <T> Streamable<T> of(Supplier<? extends Stream<T>> supplier) {
        return Wrapper.of(supplier);
    }

    /**
     * Applies a mapping function to each element and returns a new {@code Streamable} instance.
     *
     * @param mapper the mapping function
     * @return a new {@code Streamable} instance with transformed elements
     */
    default Streamable<T> map(Function<? super T, ? extends T> mapper) {
        return of(() -> stream().map(mapper));
    }

    /**
     * Filters elements based on a predicate and returns a new {@code Streamable} instance.
     *
     * @param predicate the predicate to filter elements
     * @return a new {@code Streamable} instance with filtered elements
     */
    default Streamable<T> filter(Predicate<? super T> predicate) {
        return of(() -> stream().filter(predicate));
    }

    default T reduce(T identity, BinaryOperator<T> accumulator) {
        return stream().reduce(identity, accumulator);
    }

    /**
     * Collects elements into a list.
     *
     * @return a list containing all elements
     */
    default List<T> toList() {
        return stream().collect(Collectors.toList());
    }

    /**
     * Collects elements into a set.
     *
     * @return a set containing all elements
     */
    default Set<T> toSet() {
        return stream().collect(Collectors.toSet());
    }

    /**
     * Collects elements into a map using the provided key and value mappers.
     *
     * @param keyMapper   function to generate keys
     * @param valueMapper function to generate values
     * @param <K>         key type
     * @param <V>         value type
     * @return a map containing mapped keys and values
     */
    default <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return stream().collect(Collectors.toMap(keyMapper, valueMapper));
    }

    /**
     * Collects elements into a map using the provided key mapper and identity as the value mapper.
     *
     * @param keyMapper function to generate keys
     * @param <K>       key type
     * @return a map containing mapped keys and original elements as values
     */
    default <K> Map<K, T> toMap(Function<? super T, ? extends K> keyMapper) {
        return toMap(keyMapper, Function.identity());
    }

    /**
     * Returns the stream from this {@code Streamable} instance.
     *
     * @return a sequential stream of elements
     */
    @Override
    default Stream<T> get() {
        return stream();
    }

    /**
     * Wrapper class for streamable suppliers.
     *
     * @param <T> the type of elements in the stream
     */
    class Wrapper<T> implements Streamable<T> {

        private final Supplier<? extends Stream<T>> supplier;

        private Wrapper(Supplier<? extends Stream<T>> stream) {
            this.supplier = stream;
        }

        /**
         * Creates a new wrapper for the provided stream supplier.
         *
         * @param supplier the stream supplier
         * @param <T>      the type of elements
         * @return a new {@code Wrapper} instance
         */
        public static <T> Wrapper<T> of(Supplier<? extends Stream<T>> supplier) {
            return new Wrapper<>(supplier);
        }

        @Override
        public Stream<T> stream() {
            return supplier.get();
        }

        @Override
        public Iterator<T> iterator() {
            return stream().iterator();
        }
    }
}