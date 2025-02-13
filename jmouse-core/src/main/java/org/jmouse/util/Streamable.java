package org.jmouse.util;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@FunctionalInterface
public interface Streamable<T> extends Iterable<T>, Supplier<Stream<T>> {

    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @SafeVarargs
    static <T> Streamable<T> of(T... elements) {
        return () -> List.of(elements).iterator();
    }

    @Override
    default Stream<T> get() {
        return stream();
    }

}
