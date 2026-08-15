package org.jmouse.el.template;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public interface Cache<K extends Cache.Key, V> {

    static <K extends Cache.Key, V> Cache<K, V> memory() {
        return new Memory<>();
    }

    /**
     * ⚠️ <strong>Concurrent, and it is not an optimisation.</strong>
     *
     * <p>One {@link org.jmouse.el.ExpressionLanguage} is shared by everything that compiles through
     * it, so this map is read and written by every request thread at once. A plain {@code HashMap}
     * here loses entries under a concurrent write — which shows up as an expression being reparsed
     * forever rather than as an error — and can corrupt its own table during a resize, which shows
     * up as a thread that never returns.
     */
    class Memory<K extends Cache.Key, V> implements Cache<K, V> {

        private final Map<K, V> cache = new ConcurrentHashMap<>();

        @Override
        public V get(K key) {
            return cache.get(key);
        }

        /**
         * ⚠️ A null value is not stored rather than thrown at: {@link #contains} already reads an
         * absent entry and a null one as the same thing, and {@code ConcurrentHashMap} refuses null
         * outright. Storing nothing keeps the old meaning without the new exception.
         */
        @Override
        public void put(K key, V value) {
            if (value == null) {
                return;
            }

            cache.put(key, value);
        }

    }

    V get(K key);

    default V get(K key, Supplier<V> supplier) {
        V value = get(key);

        if (value == null && supplier != null) {
            value = supplier.get();
            put(key, value);
        }

        return value;
    }

    void put(K key, V value);

    default boolean contains(K key) {
        return get(key) != null;
    }

    interface Key {

        static Key forObject(Object object) {
            return new ObjectKey(object);
        }

    }

    class ObjectKey implements Key {

        private final Object object;

        public ObjectKey(Object object) {
            this.object = object;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Cache.ObjectKey that)) {
                return false;
            }

            return Objects.equals(this.object, that.object);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            return Objects.hashCode(object) * prime;
        }

        @Override
        public String toString() {
            return "CACHE_KEY: [" + object + "]";
        }
    }

}
