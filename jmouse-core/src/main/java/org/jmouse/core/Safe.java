package org.jmouse.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Null-safe helper operations for common framework scenarios. 🛡️
 *
 * <p>
 * This utility provides compact operations for:
 * </p>
 *
 * <ul>
 *     <li>lazy fallback creation</li>
 *     <li>safe collection/map mutation</li>
 *     <li>null-safe equality and string conversion</li>
 *     <li>null-safe collection access</li>
 *     <li>defensive casting</li>
 * </ul>
 *
 * <p>
 * The goal is to reduce repetitive {@code null} checks in internal code
 * without introducing overly clever abstractions.
 * </p>
 */
public final class Safe {

    private Safe() {
    }

    /**
     * Returns the given value if it is not {@code null},
     * otherwise obtains a fallback from the supplier. 🧩
     *
     * @param value existing value
     * @param supplier fallback supplier
     * @param <T> value type
     *
     * @return existing value or supplied fallback
     */
    public static <T> T lazy(T value, Supplier<? extends T> supplier) {
        Verify.nonNull(supplier, "supplier");
        return value != null ? value : supplier.get();
    }

    /**
     * Ensures that the given collection exists. 🧱
     *
     * @param collection target collection
     * @param supplier collection factory
     * @param <E> element type
     * @param <C> collection type
     *
     * @return existing or newly created collection
     */
    public static <E, C extends Collection<E>> C ensure(C collection, Supplier<? extends C> supplier) {
        return lazy(collection, supplier);
    }

    /**
     * Ensures that the given map exists. 🗂️
     *
     * @param map target map
     * @param supplier map factory
     * @param <K> key type
     * @param <V> value type
     * @param <M> map type
     *
     * @return existing or newly created map
     */
    public static <K, V, M extends Map<K, V>> M ensure(M map, Supplier<? extends M> supplier) {
        return lazy(map, supplier);
    }

    /**
     * Adds an element into the collection, creating it first if needed. ➕
     *
     * @param collection target collection
     * @param supplier collection factory
     * @param element element to add
     * @param <E> element type
     * @param <C> collection type
     *
     * @return existing or newly created collection after mutation
     */
    public static <E, C extends Collection<E>> C add(
            C collection,
            Supplier<? extends C> supplier,
            E element
    ) {
        C target = ensure(collection, supplier);
        target.add(element);
        return target;
    }

    /**
     * Puts a key-value pair into the map, creating it first if needed. 🧩
     *
     * @param map target map
     * @param supplier map factory
     * @param key key to put
     * @param value value to put
     * @param <K> key type
     * @param <V> value type
     * @param <M> map type
     *
     * @return existing or newly created map after mutation
     */
    public static <K, V, M extends Map<K, V>> M put(
            M map,
            Supplier<? extends M> supplier,
            K key,
            V value
    ) {
        M target = ensure(map, supplier);
        target.put(key, value);
        return target;
    }

    /**
     * Returns a value from the given map or {@code null} if the map is {@code null}. 🔎
     *
     * @param map source map
     * @param key lookup key
     * @param <K> key type
     * @param <V> value type
     *
     * @return mapped value or {@code null}
     */
    public static <K, V> V get(Map<K, V> map, K key) {
        return map == null ? null : map.get(key);
    }

    /**
     * Returns {@code true} if both values are equal, handling {@code null} safely. ⚖️
     *
     * @param left first value
     * @param right second value
     *
     * @return {@code true} if both values are equal
     */
    public static boolean equals(Object left, Object right) {
        return Objects.equals(left, right);
    }

    /**
     * Returns the string representation of the value,
     * or {@code null} if the value is {@code null}. 📝
     *
     * @param value source value
     *
     * @return string value or {@code null}
     */
    public static String string(Object value) {
        return string(value, null);
    }

    /**
     * Returns the string representation of the value,
     * or the provided fallback if the value is {@code null}. 🧾
     *
     * @param value source value
     * @param fallback fallback string
     *
     * @return string value or fallback
     */
    public static String string(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    /**
     * Returns the size of the collection or {@code 0} if it is {@code null}. 📏
     *
     * @param collection source collection
     *
     * @return collection size or {@code 0}
     */
    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    /**
     * Returns the size of the map or {@code 0} if it is {@code null}. 📐
     *
     * @param map source map
     *
     * @return map size or {@code 0}
     */
    public static int size(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    /**
     * Returns {@code true} if the collection is {@code null} or empty. 📭
     *
     * @param collection source collection
     *
     * @return {@code true} if the collection is empty
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns {@code true} if the map is {@code null} or empty. 🗃️
     *
     * @param map source map
     *
     * @return {@code true} if the map is empty
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Returns the first list element or {@code null} if the list is {@code null} or empty. 🥇
     *
     * @param list source list
     * @param <T> element type
     *
     * @return first element or {@code null}
     */
    public static <T> T first(List<T> list) {
        return list == null || list.isEmpty() ? null : list.getFirst();
    }

    /**
     * Returns a stream over the given collection,
     * or an empty stream if the collection is {@code null}. 🌊
     *
     * @param collection source collection
     * @param <T> element type
     *
     * @return collection stream or empty stream
     */
    public static <T> Stream<T> stream(Collection<T> collection) {
        return collection == null ? Stream.empty() : collection.stream();
    }

    /**
     * Casts the value to the target type if compatible,
     * otherwise returns {@code null}. 🎯
     *
     * @param value source value
     * @param type target type
     * @param <T> target type
     *
     * @return cast value or {@code null}
     */
    public static <T> T cast(Object value, Class<T> type) {
        Verify.nonNull(type, "type");
        return type.isInstance(value) ? type.cast(value) : null;
    }

    /**
     * Returns the existing mapped value or computes and stores a new one if absent.
     *
     * <p>
     * If the target map is {@code null}, it is created first.
     * </p>
     *
     * @param map target map
     * @param supplier map factory
     * @param key target key
     * @param mappingFunction value factory
     * @param <K> key type
     * @param <V> value type
     * @param <M> map type
     *
     * @return existing or newly computed value
     */
    public static <K, V, M extends Map<K, V>> V computeIfAbsent(
            M map,
            Supplier<? extends M> supplier,
            K key,
            Function<? super K, ? extends V> mappingFunction
    ) {
        Verify.nonNull(supplier, "supplier");
        Verify.nonNull(mappingFunction, "mappingFunction");
        M target = ensure(map, supplier);
        return target.computeIfAbsent(key, mappingFunction);
    }

}