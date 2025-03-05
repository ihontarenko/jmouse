package org.jmouse.template;

import java.util.Objects;
import java.util.function.Supplier;

public interface Cache<K extends Cache.CacheKey, V> {

    CacheValue<V> get(K key);

    default CacheValue<V> get(K key, Supplier<CacheValue<V>> supplier) {
        CacheValue<V> value = get(key);

        if (value == null) {
            value = supplier.get();
            put(key, value.getValue());
        }

        return value;
    }

    void put(K key, V value);

    default boolean contains(K key) {
        return get(key) != null;
    }

    interface CacheKey {

        static CacheKey forObject(Object object) {
            return new ObjectCacheKey(object);
        }

    }

    interface CacheValue<V> {
        V getValue();
    }

    class ObjectCacheKey implements CacheKey {

        private final Object object;

        public ObjectCacheKey(Object object) {
            this.object = object;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ObjectCacheKey that)) {
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
