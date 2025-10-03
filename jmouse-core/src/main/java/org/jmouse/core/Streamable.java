package org.jmouse.core;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * 🌀 Combines {@link Iterable} and {@link Stream}.
 * <p>Provides functional operations while remaining iterable.</p>
 *
 * @param <T> the element type
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
@FunctionalInterface
public interface Streamable<T> extends Iterable<T>, Supplier<Stream<T>> {

    /**
     * 🔍 Finds the first matching element.
     */
    static <T> Optional<T> findFirst(Iterable<T> iterable, Predicate<? super T> predicate) {
        return of(iterable).filter(predicate).stream().findFirst();
    }

    /**
     * 🔍 Finds the all matching elements.
     */
    static <T> List<T> findAll(Iterable<T> iterable, Predicate<? super T> predicate) {
        return of(iterable).filter(predicate).stream().toList();
    }

    /**
     * 🚿 Returns a sequential stream.
     */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 📦 From iterable.
     */
    static <T> Streamable<T> of(Iterable<T> iterable) {
        return iterable::iterator;
    }

    /**
     * 📦 From array.
     */
    @SafeVarargs
    static <T> Streamable<T> of(T... elements) {
        return () -> List.of(elements).iterator();
    }

    /**
     * 📦 From stream supplier.
     */
    static <T> Streamable<T> of(Supplier<? extends Stream<T>> supplier) {
        return Wrapper.of(supplier);
    }

    /**
     * 🔁 Maps values.
     */
    default <R> Streamable<R> map(Function<? super T, ? extends R> mapper) {
        return of(() -> stream().map(mapper));
    }

    /**
     * 🧽 Filters values.
     */
    default Streamable<T> filter(Predicate<? super T> predicate) {
        return of(() -> stream().filter(predicate));
    }

    /**
     * 🚫 Skip {@code null} elements in the stream.
     *
     * @return new {@link Streamable} with only non-null elements
     */
    default Streamable<T> skipNulls() {
        return of(() -> stream().filter(Objects::nonNull));
    }

    /**
     * 🚫 Skip blank (empty or whitespace-only) strings.
     *
     * <p>⚠️ Non-strings are converted via {@link String#valueOf(Object)}.</p>
     *
     * @return new {@link Streamable} of non-blank strings
     */
    default Streamable<String> skipBlank() {
        return skipNulls().map(String::valueOf).filter(s -> !s.isBlank());
    }

    /**
     * 🧮 Reduces values.
     */
    default T reduce(T identity, BinaryOperator<T> accumulator) {
        return stream().reduce(identity, accumulator);
    }

    /**
     * 🧮 Reduces values.
     */
    default Optional<T> reduce(BinaryOperator<T> accumulator) {
        return stream().reduce(accumulator);
    }

    /**
     * 🔍 Finds first.
     */
    default Optional<T> findFirst() {
        return stream().findFirst();
    }

    /**
     * 📋 Collect all elements into a {@link List}.
     *
     * @return list of all elements
     */
    default List<T> toList() {
        return stream().collect(Collectors.toList());
    }

    /**
     * 📋 Collect filtered elements into a {@link List}.
     *
     * @param predicate filter applied before collecting
     * @return list of matching elements
     */
    default List<T> toList(Predicate<? super T> predicate) {
        return of(() -> stream().filter(predicate)).toList();
    }

    /**
     * 🧩 Collect all elements into a {@link Set}.
     *
     * @return set of all elements
     */
    default Set<T> toSet() {
        return stream().collect(Collectors.toSet());
    }

    /**
     * 🧩 Collect filtered elements into a {@link Set}.
     *
     * @param predicate filter applied before collecting
     * @return set of matching elements
     */
    default Set<T> toSet(Predicate<? super T> predicate) {
        return of(() -> stream().filter(predicate)).toSet();
    }

    /**
     * 🧩 Collects to typed array.
     */
    default T[] toArray(IntFunction<T[]> generator) {
        return stream().toArray(generator);
    }

    /**
     * 🧩 Collects to object array.
     */
    default Object[] toArray() {
        return stream().toArray(Object[]::new);
    }

    /**
     * 🗺️ Collects to map with key/value mappers.
     */
    default <K, V> Map<K, V> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper
    ) {
        return stream().collect(Collectors.toMap(keyMapper, valueMapper));
    }

    /**
     * 🗺️ Collects to map with identity values.
     */
    default <K> Map<K, T> toMap(Function<? super T, ? extends K> keyMapper) {
        return toMap(keyMapper, Function.identity());
    }

    /**
     * 🗺️ Collects to map with key toString mapper and identity values.
     */
    default Map<String, T> toStringMap() {
        return toMap(Object::toString, Function.identity());
    }

    /**
     * 🔗 Concatenates the string representation of the elements, separated by the given delimiter.
     *
     * <p>Each element is converted via {@link String#valueOf(Object)} (so {@code null} becomes {@code "null"}).</p>
     *
     * @param delimiter separator between elements (e.g., {@code ", "})
     * @return joined string
     * @see java.util.stream.Collectors#joining(CharSequence)
     */
    default String joining(String delimiter) {
        return map(String::valueOf).stream().collect(Collectors.joining(delimiter));
    }

    /**
     * 🔗 Concatenates the string representation of the elements with a delimiter, and wraps
     * the result with a prefix and suffix.
     *
     * <p>Each element is converted via {@link String#valueOf(Object)} (so {@code null} becomes {@code "null"}).</p>
     *
     * @param delimiter separator between elements (e.g., {@code ", "})
     * @param prefix    string to add at the beginning (e.g., {@code "["})
     * @param suffix    string to add at the end (e.g., {@code "]"})
     * @return joined string with prefix/suffix
     * @see java.util.stream.Collectors#joining(CharSequence, CharSequence, CharSequence)
     */
    default String joining(String delimiter, String prefix, String suffix) {
        return map(String::valueOf).stream().collect(Collectors.joining(delimiter, prefix, suffix));
    }

    /**
     * ☕ Stream supplier override.
     */
    @Override
    default Stream<T> get() {
        return stream();
    }

    class Support {

        @SuppressWarnings("unchecked")
        static <T> Optional<T> toOptional(Object value) {
            if (value instanceof Optional) {
                return (Optional<T>) value;
            } else {
                return Optional.ofNullable((T)value);
            }
        }

    }

    /**
     * 🧱 Streamable wrapper from supplier.
     */
    class Wrapper<T> implements Streamable<T> {

        private final Supplier<? extends Stream<T>> supplier;

        private Wrapper(Supplier<? extends Stream<T>> stream) {
            this.supplier = stream;
        }

        /**
         * ⚙️ Creates wrapper from stream supplier.
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
