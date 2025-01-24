package svit.matcher;

import svit.matcher.ant.AntMatcher;

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
     * Returns a matcher that checks if the given string contains the specified substring.
     *
     * @param substring the substring to check for
     * @return a matcher that returns true if the string contains the given substring
     */
    public static Matcher<String> contains(String substring) {
        return new TextContainsMatcher(substring);
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
