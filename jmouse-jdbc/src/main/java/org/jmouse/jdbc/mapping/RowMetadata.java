package org.jmouse.jdbc.mapping;

import org.jmouse.core.Contract;

import java.util.List;
import java.util.Map;

public interface RowMetadata {

    /**
     * Returns a cached column index map: column label/name -> 1-based index.
     * Must be stable and must not allocate new maps on each call.
     */
    Map<String, Integer> indexMap();

    default int indexOf(String column) {
        Map<String, Integer> index = indexMap();
        Integer i = index.get(column);
        Contract.state(i != null, () -> new IllegalArgumentException("Unknown column '" + column + "'"));
        return i;
    }

    default List<String> names() {
        return List.copyOf(indexMap().keySet());
    }

    default List<Integer> indexes() {
        return List.copyOf(indexMap().values());
    }

    default int count() {
        return indexMap().size();
    }

}
