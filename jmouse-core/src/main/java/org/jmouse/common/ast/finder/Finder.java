package org.jmouse.common.ast.finder;

import java.util.Optional;

public interface Finder<R, V, C> {
    Optional<R> find(V value, C context);
}
