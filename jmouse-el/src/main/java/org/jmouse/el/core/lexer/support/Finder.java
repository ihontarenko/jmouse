package org.jmouse.el.core.lexer.support;

import java.util.Optional;

public interface Finder<R, V, S> {
    Optional<R> find(V value, S source);
}
