package org.jmouse.security.authorization;

/**
 * 🪝 Single-argument action.
 *
 * <p>A tiny functional hook used to apply side effects to a given instance.
 * Conceptually equivalent to {@code java.util.function.Consumer<T>}.</p>
 *
 * @param <T> the input type
 */
@FunctionalInterface
public interface Accepter<T> {

    /**
     * 🎯 Performs this operation on the given instance.
     *
     * @param instance the input value (may be {@code null} if the implementation allows)
     */
    void accept(T instance);
}
