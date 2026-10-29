package org.jmouse.plugin;

import java.util.function.UnaryOperator;

/** One wrapping of a service a {@link Decorations} point holds. */
public interface Decorator<T> {

    /** Where it stands in the chain: the lower, the nearer the owner's own service. */
    int order();

    T decorate(T inner);

    static <T> Decorator<T> of(int order, UnaryOperator<T> decoration) {
        return new Decorator<>() {
            @Override
            public int order() {
                return order;
            }

            @Override
            public T decorate(T inner) {
                return decoration.apply(inner);
            }
        };
    }
}
