package org.jmouse.template.lexer.support;

import java.util.Optional;

public interface Finder<R, V, S> {
    Optional<R> find(V value, S source);
}
