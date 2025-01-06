package svit.provider.data;

import java.util.Map;
import java.util.Set;

public interface DataProvider<K, V> {

    V getValue(K key);

    Map<K, V> getValuesMap();

    Set<V> getValuesSet();

}
