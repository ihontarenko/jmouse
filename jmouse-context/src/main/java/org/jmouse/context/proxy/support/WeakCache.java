package org.jmouse.context.proxy.support;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WeakCache<K, V> {

    private final Map<K, WeakReference<V>> cache = new ConcurrentHashMap<>();

    public V get(K key) {
        WeakReference<V> reference = cache.get(key);

        if (reference != null) {
            return reference.get();
        }

        return null;
    }

    public void put(K key, V value) {
        cache.put(key, new WeakReference<>(value));
    }

}
