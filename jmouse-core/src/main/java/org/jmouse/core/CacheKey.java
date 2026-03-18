package org.jmouse.core;

import java.util.Arrays;

import static org.jmouse.core.Verify.nonNull;

/**
 * Composite cache key abstraction. 🗝️
 *
 * <p>
 * {@code CacheKey} represents a structural key used for cache lookups.
 * Implementations are optimized for common key shapes such as:
 * </p>
 * <ul>
 *     <li>single key</li>
 *     <li>pair key</li>
 *     <li>arbitrary key arrays</li>
 *     <li>identity-based multi-key</li>
 * </ul>
 *
 * <p>
 * Typical usage:
 * </p>
 *
 * <pre>{@code
 * CacheKey key1 = CacheKey.of(type);
 * CacheKey key2 = CacheKey.of(type, property);
 * CacheKey key3 = CacheKey.of(type, property, locale);
 * CacheKey key4 = CacheKey.identity(instance, method);
 * }</pre>
 */
public interface CacheKey {

    /**
     * Creates a cache key with a single value.
     *
     * @param key key value
     *
     * @return cache key
     */
    static CacheKey of(Object key) {
        return new SingleKey(key);
    }

    /**
     * Creates a cache key with two values.
     *
     * @param key1 first key value
     * @param key2 second key value
     *
     * @return cache key
     */
    static CacheKey of(Object key1, Object key2) {
        return new Key12(key1, key2);
    }

    /**
     * Creates a cache key with one or more values.
     *
     * <p>
     * Uses specialized implementations for one and two values,
     * and {@link KeyN} for larger key sets.
     * </p>
     *
     * @param keys key values
     *
     * @return cache key
     */
    static CacheKey of(Object... keys) {
        if (keys.length == 1) {
            return new SingleKey(keys[0]);
        }
        if (keys.length == 2) {
            return new Key12(keys[0], keys[1]);
        }
        return new KeyN(keys);
    }

    /**
     * Creates an identity-based cache key.
     *
     * <p>
     * Key equality is based on reference identity ({@code ==})
     * instead of {@link Object#equals(Object)}.
     * </p>
     *
     * @param keys key values
     *
     * @return identity-based cache key
     */
    static CacheKey identity(Object... keys) {
        return new IdentityKeyN(keys);
    }

    /**
     * Cache key for a single value. 🧩
     */
    final class SingleKey implements CacheKey {

        private final Object key;
        private final int    hash;

        /**
         * Creates a single-value cache key.
         *
         * @param key key value
         */
        SingleKey(Object key) {
            this.key = nonNull(key, "key");
            this.hash = key.hashCode();
        }

        /**
         * Compares this key with another object.
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof SingleKey that)) {
                return false;
            }

            return key.equals(that.key);
        }

        /**
         * Returns cached hash code.
         */
        @Override
        public int hashCode() {
            return hash;
        }

        /**
         * Returns string representation of this key.
         */
        @Override
        public String toString() {
            return "SingleKey[" + key + "]";
        }
    }

    /**
     * Cache key for two values. 🧩
     */
    final class Key12 implements CacheKey {

        private final Object key1;
        private final Object key2;
        private final int    hash;

        /**
         * Creates a two-value cache key.
         *
         * @param key1 first key value
         * @param key2 second key value
         */
        Key12(Object key1, Object key2) {
            this.key1 = nonNull(key1, "key1");
            this.key2 = nonNull(key2, "key2");
            this.hash = 31 * key1.hashCode() + key2.hashCode();
        }

        /**
         * Compares this key with another object.
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof Key12 that)) {
                return false;
            }

            return key1.equals(that.key1) && key2.equals(that.key2);
        }

        /**
         * Returns cached hash code.
         */
        @Override
        public int hashCode() {
            return hash;
        }

        /**
         * Returns string representation of this key.
         */
        @Override
        public String toString() {
            return "Key12[" + key1 + ", " + key2 + "]";
        }
    }

    /**
     * Cache key for an arbitrary number of values. 🧩
     *
     * <p>
     * Equality uses structural deep comparison via {@link Arrays#deepEquals(Object[], Object[])}.
     * </p>
     */
    final class KeyN implements CacheKey {

        private final Object[] keys;
        private final int      hash;

        /**
         * Creates a multi-value cache key.
         *
         * @param keys key values
         */
        KeyN(Object[] keys) {
            this.keys = Arrays.copyOf(keys, keys.length);

            for (Object key : this.keys) {
                nonNull(key, "key");
            }

            this.hash = Arrays.deepHashCode(this.keys);
        }

        /**
         * Compares this key with another object.
         */
        @Override
        public boolean equals(Object other) {

            if (this == other) {
                return true;
            }

            if (!(other instanceof KeyN that)) {
                return false;
            }

            return Arrays.deepEquals(keys, that.keys);
        }

        /**
         * Returns cached hash code.
         */
        @Override
        public int hashCode() {
            return hash;
        }

        /**
         * Returns string representation of this key.
         */
        @Override
        public String toString() {
            return "KeyN" + Arrays.deepToString(keys);
        }
    }

    /**
     * Identity-based cache key for multiple values. 🧷
     *
     * <p>
     * Equality compares key references using {@code ==} and hash code
     * is built from {@link System#identityHashCode(Object)}.
     * </p>
     */
    final class IdentityKeyN implements CacheKey {

        private final Object[] keys;
        private final int      hash;

        /**
         * Creates an identity-based multi-value cache key.
         *
         * @param keys key values
         */
        IdentityKeyN(Object[] keys) {
            this.keys = Arrays.copyOf(keys, keys.length);

            int result = 1;

            for (Object key : this.keys) {
                result = 31 * result + System.identityHashCode(key);
            }

            this.hash = result;
        }

        /**
         * Compares this key with another object using identity semantics.
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof IdentityKeyN that)) {
                return false;
            }

            if (keys.length != that.keys.length) {
                return false;
            }

            for (int i = 0; i < keys.length; i++) {
                if (keys[i] != that.keys[i]) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Returns cached hash code.
         */
        @Override
        public int hashCode() {
            return hash;
        }

        /**
         * Returns string representation of this key.
         */
        @Override
        public String toString() {
            return "IdentityKeyN" + Arrays.deepToString(keys);
        }
    }

}