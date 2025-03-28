package org.jmouse.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public interface Cache<K extends Cache.Key, V> {

    static <K extends Cache.Key, V> Cache<K, V> memory() {
        return new Memory<>();
    }

    class Memory<K extends Cache.Key, V> implements Cache<K, V> {

        private final Map<K, V> cache = new HashMap<>();

        @Override
        public V get(K key) {
            return cache.get(key);
        }

        @Override
        public void put(K key, V value) {
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
    }

}
