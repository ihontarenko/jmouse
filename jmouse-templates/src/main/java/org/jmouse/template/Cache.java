package org.jmouse.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public interface Cache<K extends Cache.Key, V> {

    class Memory<K extends Cache.Key, V> implements Cache<K, V> {

        private final Map<K, V> cache = new HashMap<>();

        @Override
        public Value<V> get(K key) {
            return Value.forObject(cache.get(key));
        }

        @Override
        public void put(K key, V value) {

        }

    }

    Value<V> get(K key);

    default Value<V> get(K key, Supplier<V> supplier) {
        Value<V> value = get(key);

        if (value == null && supplier != null) {
            value = Value.forObject(supplier.get());
            if (value.isPresent()) {
                put(key, value.getValue());
            }
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

    interface Value<V> {

        static <T> Value<T> forObject(T object) {
            return new ObjectValue<>(object);
        }

        V getValue();

        default boolean isPresent() {
            return getValue() != null;
        }

    }

    class ObjectValue<T> implements Value<T> {

        private final T value;

        public ObjectValue(T value) {
            this.value = value;
        }

        @Override
        public T getValue() {
            return value;
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
