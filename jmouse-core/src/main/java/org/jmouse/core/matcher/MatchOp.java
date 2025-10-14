package org.jmouse.core.matcher;

/**
 * 🎯 Functional interface representing a single match operation.
 *
 * @param <T> input type
 * @param <R> result type (null when not matched)
 */
@FunctionalInterface
public interface MatchOp<T, R> {

    /**
     * @return result if matched, or {@code null} otherwise
     */
    R match(T value);
}