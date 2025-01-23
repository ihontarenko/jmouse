package svit.support.provider.data;

import java.util.*;

abstract public class AbstractDataProvider<K, V> implements DataProvider<K, V> {

    protected final Map<K, V> keyValues;

    protected AbstractDataProvider(Map<K, V> keyValues) {
        this.keyValues = keyValues;
    }

    @Override
    public V getValue(K key) {
        return keyValues.get(key);
    }

    @Override
    public Map<K, V> getValuesMap() {
        return Collections.unmodifiableMap(keyValues);
    }

    @Override
    public Set<V> getValuesSet() {
        return Set.copyOf(keyValues.values());
    }

}
