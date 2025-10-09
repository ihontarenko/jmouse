package org.jmouse.core.matcher;

import org.jmouse.core.matcher.ant.AntMatcher;

import java.util.stream.Stream;

/**
 * A utility class that provides matchers for strings. These matchers can check if a string contains
 * a substring, equals another string, starts with a prefix, or ends with a suffix.
 */
public class TextMatchers {

    /**
     * Creates a matcher that evaluates strings against an Ant-style pattern.
     *
     * @param pattern the Ant-style pattern to match against (e.g., "*.txt", "/path/**")
     * @return a {@link Matcher} that evaluates whether strings match the specified pattern
     * <p>
     * The Ant-style pattern supports wildcards (`*`, `**`) and is useful for flexible string matching, such as file paths.
     * </p>
     * Example:
     * <pre>{@code
     * Matcher<String> matcher = TextMatchers.ant("*.java");
     * System.out.println(matcher.matches("Test.java")); // true
     * System.out.println(matcher.matches("Test.class")); // false
     * }</pre>
     */
    public static Matcher<String> ant(String pattern) {
        return new AntMatcher(pattern);
    }

    /**
     * 🔍 Creates a matcher that checks if a string contains the specified substring.
     *
     * @param substring the substring to search for
     * @return a matcher that returns {@code true} if the string contains the substring
     */
    public static Matcher<String> contains(String substring) {
        return new TextContainsMatcher(substring);
    }

    /**
     * 🔎 Creates a matcher that checks if a string contains <em>any</em> of the specified substrings.
     *
     * @param substring one or more substrings to check for
     * @return a matcher that returns {@code true} if the string contains at least one of the substrings
     */
    public static Matcher<String> containsAny(String... substring) {
        return new TextContainsAnyMatcher(substring);
    }

    /**
     * 📝 Creates a matcher that checks if a string is {@code null} or blank
     * (empty or whitespace-only).
     *
     * @return a matcher that returns {@code true} if the string is blank
     */
    public static Matcher<String> blank() {
        return (string) -> string == null || string.isBlank();
    }

    /**
     * 🟢 Creates a matcher that checks if a string is non-blank
     * (not {@code null}, not empty, and contains non-whitespace characters).
     *
     * @return a matcher that returns {@code true} if the string is not blank
     */
    public static Matcher<String> notBlank() {
        return blank().not();
    }

    /**
     * Returns a matcher that checks if the given string is equal to the specified string.
     *
     * @param substring the string to compare
     * @return a matcher that returns true if the string is exactly the same as the specified string
     */
    public static Matcher<String> same(String substring) {
        return new TextEqualsMatcher(substring);
    }

    /**
     * Returns a matcher that checks if the given string starts with the specified prefix.
     *
     * @param prefix the prefix to check for
     * @return a matcher that returns true if the string starts with the given prefix
     */
    public static Matcher<String> startsWith(String prefix) {
        return new TextStartsWithMatcher(prefix);
    }

    /**
     * Returns a matcher that checks if the given string ends with the specified suffix.
     *
     * @param suffix the suffix to check for
     * @return a matcher that returns true if the string ends with the given suffix
     */
    public static Matcher<String> endsWith(String suffix) {
        return new TextEndsWithMatcher(suffix);
    }

    /**
     * A matcher that checks if the given string is exactly the same as the specified string.
     */
    private record TextEqualsMatcher(String string) implements Matcher<String> {

        @Override
        public boolean matches(String item) {
            return item != null && item.equals(string);
        }

        @Override
        public String toString() {
            return "EQUALS [ %s ]".formatted(string);
        }
    }

    /**
     * A matcher that checks if the given string contains the specified substring.
     */
    private record TextContainsMatcher(String substring) implements Matcher<String> {

        @Override
        public boolean matches(String item) {
            return item != null && item.contains(substring);
        }

        @Override
        public String toString() {
            return "CONTAINS [ %s ]".formatted(substring);
        }
    }

    private record TextContainsAnyMatcher(String[] substrings, Matcher<String> matcher) implements Matcher<String> {

        public TextContainsAnyMatcher(String... substrings) {
            this(substrings, Stream.of(substrings)
                    .map(TextMatchers::contains)
                    .reduce(Matcher::or)
                    .orElse(Matcher.constant(true)));
        }

        @Override
        public boolean matches(String item) {
            return item != null && matcher.matches(item);
        }

        @Override
        public String toString() {
            return "CONTAINS ANY [ %s ]".formatted(matcher);
        }
    }

    /**
     * A matcher that checks if the given string starts with the specified prefix.
     */
    private record TextStartsWithMatcher(String prefix) implements Matcher<String> {

        @Override
        public boolean matches(String item) {
            return item != null && item.startsWith(prefix);
        }

        @Override
        public String toString() {
            return "STARTS [ %s ]".formatted(prefix);
        }
    }

    /**
     * A matcher that checks if the given string ends with the specified suffix.
     */
    private record TextEndsWithMatcher(String suffix) implements Matcher<String> {

        @Override
        public boolean matches(String item) {
            return item != null && item.endsWith(suffix);
        }

        @Override
        public String toString() {
            return "ENDS [ %s ]".formatted(suffix);
        }
    }

}
