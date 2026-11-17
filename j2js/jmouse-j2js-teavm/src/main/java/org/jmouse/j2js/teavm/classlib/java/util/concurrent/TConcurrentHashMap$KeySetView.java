package org.jmouse.j2js.teavm.classlib.java.util.concurrent;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code java.util.concurrent.ConcurrentHashMap.KeySetView} in the browser: the keys of a map as a set, an added key mapped to
 * the view's value — as the JDK's. Named as TeaVM names the substitute of a nested class: its outer class's substitute, then
 * {@code $} and its own name. It also gives {@code ConcurrentHashMap} its sets of keys.
 *
 * @param <K> the keys
 * @param <V> the value an added key is mapped to
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(ConcurrentHashMap.class)
public final class TConcurrentHashMap$KeySetView<K, V> extends AbstractSet<K> {

    private final ConcurrentHashMap<K, V> map;
    private final V                       mappedValue;

    public TConcurrentHashMap$KeySetView(ConcurrentHashMap<K, V> map, V mappedValue) {
        this.map = Objects.requireNonNull(map);
        this.mappedValue = mappedValue;
    }

    /** A set backed by a new map, an added key mapped to {@code true}: {@code ConcurrentHashMap.newKeySet()}. */
    @Supplement(SupplementKind.STATIC)
    public static <K> TConcurrentHashMap$KeySetView<K, Boolean> newKeySet() {
        return new TConcurrentHashMap$KeySetView<>(new ConcurrentHashMap<>(), Boolean.TRUE);
    }

    @Supplement(SupplementKind.STATIC)
    public static <K> TConcurrentHashMap$KeySetView<K, Boolean> newKeySet(int initialCapacity) {
        return new TConcurrentHashMap$KeySetView<>(new ConcurrentHashMap<>(initialCapacity), Boolean.TRUE);
    }

    /** The keys of {@code map}; none can be added: {@code ConcurrentHashMap.keySet()}, as the JDK types it. */
    @Supplement(SupplementKind.INSTANCE)
    public static <K, V> TConcurrentHashMap$KeySetView<K, V> keySet(ConcurrentHashMap<K, V> map) {
        return new TConcurrentHashMap$KeySetView<>(map, null);
    }

    /** The keys of {@code map}, an added key mapped to {@code mappedValue}: {@code ConcurrentHashMap.keySet(V)}. */
    @Supplement(SupplementKind.INSTANCE)
    public static <K, V> TConcurrentHashMap$KeySetView<K, V> keySet(ConcurrentHashMap<K, V> map, V mappedValue) {
        return new TConcurrentHashMap$KeySetView<>(map, Objects.requireNonNull(mappedValue));
    }

    /** The map the keys are of. */
    public ConcurrentHashMap<K, V> getMap() {
        return map;
    }

    /** What an added key is mapped to; null where keys cannot be added. */
    public V getMappedValue() {
        return mappedValue;
    }

    @Override
    public boolean add(K key) {
        if (mappedValue == null) {
            throw new UnsupportedOperationException();
        }
        return map.putIfAbsent(key, mappedValue) == null;
    }

    @Override
    public boolean addAll(Collection<? extends K> keys) {
        boolean added = false;
        for (K key : keys) {
            added |= add(key);
        }
        return added;
    }

    @Override
    public boolean contains(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean remove(Object key) {
        return map.remove(key) != null;
    }

    @Override
    public Iterator<K> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void clear() {
        map.clear();
    }
}
