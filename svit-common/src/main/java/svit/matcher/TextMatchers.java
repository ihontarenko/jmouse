package svit.matcher;

/**
 * A utility class that provides matchers for strings. These matchers can check if a string contains
 * a substring, equals another string, starts with a prefix, or ends with a suffix.
 */
public class TextMatchers {

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
    }

    /**
     * A matcher that checks if the given string contains the specified substring.
     */
    private record TextContainsMatcher(String substring) implements Matcher<String> {
        @Override
        public boolean matches(String item) {
            return item != null && item.contains(substring);
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
    }

    /**
     * A matcher that checks if the given string ends with the specified suffix.
     */
    private record TextEndsWithMatcher(String suffix) implements Matcher<String> {
        @Override
        public boolean matches(String item) {
            return item != null && item.endsWith(suffix);
        }
    }

}
