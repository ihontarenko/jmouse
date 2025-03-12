package org.jmouse.template;

import java.util.Objects;
import java.util.function.Supplier;

public interface Cache<K extends Cache.Key, V> {

    Value<V> get(K key);

    default Value<V> get(K key, Supplier<Value<V>> supplier) {
        Value<V> value = get(key);

        if (value == null) {
            value = supplier.get();
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

        V getValue();

        default boolean isPresent() {
            return getValue() != null;
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
