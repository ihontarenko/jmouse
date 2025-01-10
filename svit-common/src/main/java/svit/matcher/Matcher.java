package svit.matcher;

import java.util.Arrays;
/**
 * A generic interface for matching items of type {@code T} against certain conditions.
 * This interface supports chaining of matchers using logical operators such as AND, OR, XOR, and NOT.
 *
 * @param <T> the type of the object being matched
 */
@SuppressWarnings({"unused"})
public interface Matcher<T> {

    /**
     * Evaluates whether the given {@code item} matches the criteria defined by this matcher.
     *
     * @param item the object to be evaluated
     * @return {@code true} if the item matches the criteria, {@code false} otherwise
     */
    boolean matches(T item);

    /**
     * Combines this matcher with another matcher using the logical AND operator.
     * Both matchers must return true for the final result to be true.
     *
     * @param other the other matcher to combine with
     * @return a new matcher that represents the logical AND of the two matchers
     * @see #logicalAnd(Matcher, Matcher)
     * @example
     * <pre>{@code
     * Matcher<String> startsWithA = item -> item.startsWith("A");
     * Matcher<String> endsWithZ = item -> item.endsWith("Z");
     * Matcher<String> combinedMatcher = startsWithA.and(endsWithZ);
     * combinedMatcher.matches("AZ"); // returns true
     * combinedMatcher.matches("BZ"); // returns false
     * }</pre>
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
     * @example
     * <pre>{@code
     * Matcher<String> startsWithA = item -> item.startsWith("A");
     * Matcher<String> endsWithZ = item -> item.endsWith("Z");
     * Matcher<String> combinedMatcher = startsWithA.or(endsWithZ);
     * combinedMatcher.matches("AZ"); // returns true
     * combinedMatcher.matches("BZ"); // returns true
     * combinedMatcher.matches("XY"); // returns false
     * }</pre>
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
     * @example
     * <pre>{@code
     * Matcher<String> startsWithA = item -> item.startsWith("A");
     * Matcher<String> endsWithZ = item -> item.endsWith("Z");
     * Matcher<String> combinedMatcher = startsWithA.xor(endsWithZ);
     * combinedMatcher.matches("AZ"); // returns false
     * combinedMatcher.matches("A");  // returns true
     * combinedMatcher.matches("Z");  // returns true
     * }</pre>
     */
    default Matcher<T> xor(Matcher<? super T> other) {
        return logicalXor(this, other);
    }

    /**
     * Negates the result of this matcher using the logical NOT operator.
     *
     * @return a new matcher that represents the negation of this matcher
     * @example
     * <pre>{@code
     * Matcher<String> startsWithA = item -> item.startsWith("A");
     * Matcher<String> notMatcher = startsWithA.not();
     * notMatcher.matches("AZ"); // returns false
     * notMatcher.matches("BZ"); // returns true
     * }</pre>
     */
    default Matcher<T> not() {
        return not(this);
    }

    /**
     * Combines two matchers using the logical AND operator.
     * Both matchers must return true for the final result to be true.
     *
     * @param first the first matcher
     * @param second the second matcher
     * @return a new matcher that represents the logical AND of the two matchers
     * @example
     * <pre>{@code
     * Matcher<String> startsWithA = item -> item.startsWith("A");
     * Matcher<String> endsWithZ = item -> item.endsWith("Z");
     * Matcher<String> combinedMatcher = Matcher.and(startsWithA, endsWithZ);
     * combinedMatcher.matches("AZ"); // returns true
     * }</pre>
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
     * @example
     * <pre>{@code
     * Matcher<Field> matcher = Matcher.constant(false);
     * for (Class<? extends Annotation> annotation : annotations) {
     *      matcher = matcher.or(FieldMatchers.isAnnotatedWith(annotation));
     * }
     * matcher.matches(someField); // Will return true if the field has any of the annotations
     * }</pre>
     */
    static <T> Matcher<T> constant(boolean value) {
        return item -> value;
    }

    /**
     * A matcher that combines two matchers using the logical AND operator.
     * Both matchers must return true for the final result to be true.
     *
     * @param <T> the type of the object being matched
     */
    record AndMatcher<T>(Matcher<? super T> left, Matcher<? super T> right)
            implements Matcher<T> {

        @Override
        public boolean matches(T item) {
            return left.matches(item) && right.matches(item);
        }

        @Override
        public String toString() {
            return "AND";
        }
    }

    /**
     * A matcher that combines two matchers using the logical OR operator.
     * At least one matcher must return true for the final result to be true.
     *
     * @param <T> the type of the object being matched
     */
    record OrMatcher<T>(Matcher<? super T> left, Matcher<? super T> right)
            implements Matcher<T> {

        @Override
        public boolean matches(T item) {
            return left.matches(item) || right.matches(item);
        }

        @Override
        public String toString() {
            return "OR";
        }
    }

    /**
     * A matcher that combines two matchers using the logical XOR operator.
     * The final result is true if one matcher returns true and the other returns false.
     *
     * @param <T> the type of the object being matched
     */
    record XorMatcher<T>(Matcher<? super T> left, Matcher<? super T> right)
            implements Matcher<T> {

        @Override
        public boolean matches(T item) {
            return left.matches(item) ^ right.matches(item);
        }

        @Override
        public String toString() {
            return "XOR";
        }
    }

    /**
     * A matcher that negates the result of another matcher using the logical NOT operator.
     *
     * @param <T> the type of the object being matched
     */
    record NotMatcher<T>(Matcher<? super T> matcher) implements Matcher<T> {

        @Override
        public boolean matches(T item) {
            return !matcher.matches(item);
        }

        @Override
        public String toString() {
            return "NOT";
        }
    }

}
