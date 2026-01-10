package org.jmouse.common.ast.finder;

import java.util.List;
import java.util.Optional;

public class ListFinder<V> implements Finder<Integer, V, List<V>> {

    @Override
    public Optional<Integer> find(V value, List<V> context) {
        return Optional.of(context.indexOf(value));
    }

}
