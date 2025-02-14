package org.jmouse.core.bind;

import java.util.function.Supplier;

@FunctionalInterface
public interface BeanConstructor<T, A> {
    Supplier<T> construct(A context);
}
