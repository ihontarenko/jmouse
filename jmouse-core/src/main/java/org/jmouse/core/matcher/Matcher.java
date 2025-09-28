package org.jmouse.core.matcher;

import java.util.Arrays;
/**
 * A generic interface for matching items of type {@code T} against certain conditions.
 * This interface supports chaining of matchers using logical operators such as AND, OR, XOR, and NOT.
 *
 * @param <T> the type of the structured being matched
 */
@SuppressWarnings({"unused"})
public interface Matcher<T> {

    /**
     * Evaluates whether the given {@code item} matches the criteria defined by this matcher.
     *
     * @param item the structured to be evaluated
     * @return {@code true} if the item matches the criteria, {@code false} otherwise
     */
    boolean matches(T item);

    static  <T, U> Matcher<U> asType(Matcher<T> matcher, Class<T> type) {
        return item -> matcher.matches((T) item);
    }

    /**
     * Combines this matcher with another matcher using the logical AND operator.
     * Both matchers must return true for the final result to be true.
     *
     * @param other the other matcher to combine with
     * @return a new matcher that represents the logical AND of the two matchers
     * @see #logicalAnd(Matcher, Matcher)
     */
    default Matcher<T> and(Matcher<? super T> other) {
        return logicalAnd(this, other);
    }

    /**
     * Combines this matcher with another matcher using the logical OR operator.
     * At least one matcher must return true for the final result to be true.
     *
     * @param other the other matcher to combine with
     * @return a new matcher that represents the logical OR of the two matchers
     * @see #logicalOr(Matcher...)
     */
    default Matcher<T> or(Matcher<? super T> other) {
        return logicalOr(this, other);
    }

    /**
     * Combines this matcher with another matcher using the logical XOR operator.
     * The final result is true if one matcher returns true and the other returns false.
     *
     * @param other the other matcher to combine with
     * @return a new matcher that represents the logical XOR of the two matchers
     * @see #logicalXor(Matcher, Matcher)
     */
    default Matcher<T> xor(Matcher<? super T> other) {
        return logicalXor(this, other);
    }

    /**
     * Negates the result of this matcher using the logical NOT operator.
     *
     * @return a new matcher that represents the negation of this matcher
     */
    default Matcher<T> not() {
        return not(this);
    }

    /**
     * 🎯 Narrow the type of this matcher.
     *
     * <p>Useful when a matcher is defined for a supertype but needs to be
     * applied to a subtype without casts.</p>
     *
     * <pre>{@code
     * Matcher<Executable> any = ...;
     * Matcher<Method> methodsOnly = any.narrow();
     * }</pre>
     *
     * @param <S> narrowed subtype
     * @return the same matcher but typed to {@code S}
     */
    default <S extends T> Matcher<S> narrow() {
        return this::matches;
    }

    /**
     * Combines two matchers using the logical AND operator.
     * Both matchers must return true for the final result to be true.
     *
     * @param first the first matcher
     * @param second the second matcher
     * @return a new matcher that represents the logical AND of the two matchers
     */
    static <T> Matcher<T> logicalAnd(Matcher<? super T> first, Matcher<? super T> second) {
        return new AndMatcher<>(first, second);
    }

    /**
     * Combines set of matchers using the logical AND operator.
     * All matchers must return true for the final result to be true.
     *
     * @param matchers the array of matchers
     * @return a new matcher that represents the logical AND of the set of matchers
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T> Matcher<T> logicalAnd(Matcher<? super T>... matchers) {
        return (Matcher<T>) Arrays.stream(matchers).reduce(Matcher.constant(true), Matcher::logicalAnd);
    }

    /**
     * Combines two matchers using the logical OR operator.
     * At least one matcher must return true for the final result to be true.
     *
     * @param first the first matcher
     * @param second the second matcher
     * @return a new matcher that represents the logical OR of the two matchers
     */
    static <T> Matcher<T> logicalOr(Matcher<? super T> first, Matcher<? super T> second) {
        return new OrMatcher<>(first, second);
    }

    /**
     * Combines set of matchers using the logical OR operator.
     * All matchers must return true for the final result to be true.
     *
     * @param matchers the array of matchers
     * @return a new matcher that represents the logical OR of the set of matchers
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T> Matcher<T> logicalOr(Matcher<? super T>... matchers) {
        return (Matcher<T>) Arrays.stream(matchers).reduce(Matcher.constant(false), Matcher::logicalOr);
    }

    /**
     * Combines two matchers using the logical XOR operator.
     * The final result is true if one matcher returns true and the other returns false.
     *
     * @param first the first matcher
     * @param second the second matcher
     * @return a new matcher that represents the logical XOR of the two matchers
     */
    static <T> Matcher<T> logicalXor(Matcher<? super T> first, Matcher<? super T> second) {
        return new XorMatcher<>(first, second);
    }

    /**
     * Negates the result of the given matcher using the logical NOT operator.
     *
     * @param matcher the matcher to negate
     * @return a new matcher that represents the negation of the given matcher
     */
    static <T> Matcher<T> not(Matcher<? super T> matcher) {
        return new NotMatcher<>(matcher);
    }

    /**
     * Returns a matcher that always returns the given boolean value.
     * Useful when we need to combine matchers one-by-one in loops.
     *
     * @param value the boolean value to return
     * @return a matcher that always returns {@code value}
     */
    static <T> Matcher<T> constant(boolean value) {
        return new Constant<>(value);
    }

    /**
     * A {@link Matcher} implementation that always returns a constant boolean value.
     *
     * @param <T> the type of items to be matched
     * @param constant the boolean value to return for all matches
     */
    record Constant<T>(boolean constant) implements Matcher<T> {

        /**
         * Matches the given item by returning the constant boolean value.
         *
         * @param item the item to match (ignored in this implementation)
         * @return the constant boolean value
         */
        @Override
        public boolean matches(T item) {
            return constant;
        }

        /**
         * Returns a string representation of this matcher.
         *
         * @return a string in the format "BOOLEAN [ true ]" or "BOOLEAN [ false ]"
         */
        @Override
        public String toString() {
            return "BOOLEAN [ %s ]".formatted(constant);
        }
    }


    /**
     * A matcher that combines two matchers using the logical AND operator.
     * Both matchers must return true for the final result to be true.
     *
     * @param <T> the type of the structured being matched
     */
    record AndMatcher<T>(Matcher<? super T> left, Matcher<? super T> right)
            implements Matcher<T> {

        @Override
        public boolean matches(T item) {
            return left.matches(item) && right.matches(item);
        }

        @Override
        public String toString() {
            return "[ %s AND %s ]".formatted(left, right);
        }
    }

    /**
     * A matcher that combines two matchers using the logical OR operator.
     * At least one matcher must return true for the final result to be true.
     *
     * @param <T> the type of the structured being matched
     */
    record OrMatcher<T>(Matcher<? super T> left, Matcher<? super T> right)
            implements Matcher<T> {

        @Override
        public boolean matches(T item) {
            return left.matches(item) || right.matches(item);
        }

        @Override
        public String toString() {
            return "[ %s OR %s ]".formatted(left, right);
        }
    }

    /**
     * A matcher that combines two matchers using the logical XOR operator.
     * The final result is true if one matcher returns true and the other returns false.
     *
     * @param <T> the type of the structured being matched
     */
    record XorMatcher<T>(Matcher<? super T> left, Matcher<? super T> right)
            implements Matcher<T> {

        @Override
        public boolean matches(T item) {
            return left.matches(item) ^ right.matches(item);
        }

        @Override
        public String toString() {
            return "[ %s XOR %s ]".formatted(left, right);
        }
    }

    /**
     * A matcher that negates the result of another matcher using the logical NOT operator.
     *
     * @param <T> the type of the structured being matched
     */
    record NotMatcher<T>(Matcher<? super T> matcher) implements Matcher<T> {

        @Override
        public boolean matches(T item) {
            return !matcher.matches(item);
        }

        @Override
        public String toString() {
            return "NOT [ %s ]".formatted(matcher);
        }
    }

}
