package org.jmouse.core.matcher;

/**
 * 🎯 Functional interface representing a single match operation.
 *
 * <p>Intended as a lightweight abstraction for performing a match or evaluation
 * on an input value and optionally producing a result.
 * It complements {@link org.jmouse.core.matcher.Matcher} by allowing
 * functional-style operations that return structured results rather than
 * simple boolean values.</p>
 *
 * <pre>{@code
 * MatchOp<String> op = value -> value.isEmpty() ? null : "MATCHED";
 * String result = op.match("hello"); // → "MATCHED"
 * }</pre>
 *
 * @param <T> the type of value to be matched
 * @see org.jmouse.core.matcher.Matcher
 */
@FunctionalInterface
public interface MatchOp<T, R> {

    /**
     * 🧩 Performs the match operation on the given value.
     *
     * @param value the value to evaluate
     * @param <R>   the type of the result returned by the operation
     * @return a result if matched, or {@code null} otherwise
     */
    Match match(T value);
}
