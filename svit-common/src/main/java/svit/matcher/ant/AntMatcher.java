package svit.matcher.ant;

import svit.matcher.Matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A matcher for strings based on Ant-style patterns.
 * <p>
 * This matcher supports wildcards for flexible string matching:
 * <ul>
 *     <li>{@code ?} - Matches exactly one character.</li>
 *     <li>{@code *} - Matches zero or more characters in a single segment.</li>
 *     <li>{@code **} - Matches zero or more segments in a path.</li>
 * </ul>
 * Example:
 * <pre>{@code
 * new AntMatcher("*.java").matches("Example.java"); // true
 * }</pre>
 */
final public class AntMatcher implements Matcher<String> {

    public static final String ANY_CHARACTER      = "?";
    public static final String ANY_SINGLE_SEGMENT = "*";
    public static final String ANY_MULTI_SEGMENT  = "**";

    private final Collection<String> patterns = new ArrayList<>();
    private final String             token;

    /**
     * Constructs an {@link AntMatcher} with the specified pattern and token.
     *
     * @param pattern the Ant-style pattern to use
     * @param token   the delimiter for splitting segments (e.g., "/")
     * @throws NullPointerException if the pattern is null
     */
    public AntMatcher(String pattern, String token) {
        Objects.requireNonNull(pattern, "Pattern must not be null");
        this.patterns.addAll(List.of(pattern.split(token)));
        this.token = token;
    }

    /**
     * Constructs an {@link AntMatcher} with the specified pattern.
     * The default token is {@code "/"}.
     *
     * @param pattern the Ant-style pattern to use
     * @throws NullPointerException if the pattern is null
     */
    public AntMatcher(String pattern) {
        this(pattern, "/");
    }

    /**
     * Determines if the specified item matches the pattern.
     *
     * @param item the string to evaluate
     * @return {@code true} if the item matches the pattern, {@code false} otherwise
     * <p>
     * This method splits the input into segments using the specified token and compares each segment
     * to the corresponding pattern segment. Supports multi-segment matching with {@code **}.
     * </p>
     */
    @Override
    public boolean matches(String item) {
        List<String> segments = new ArrayList<>(List.of(item.split(token)));
        List<String> patterns = new ArrayList<>(this.patterns);

        if (segments.size() < patterns.size()) {
            return false;
        }

        segments.removeIf(String::isBlank);
        patterns.removeIf(String::isBlank);

        int i = 0, j = 0;

        while (i < segments.size() && j < patterns.size()) {
            String segment = segments.get(i);
            String pattern = patterns.get(j);

            if (pattern.equals(ANY_MULTI_SEGMENT)) {
                // ** matches any sequence of segments
                if (segments.size() == j + 1 || patterns.size() <= j + 1) {
                    return true;
                }

                while (i < segments.size()) {
                    // find match with next pattern
                    String np = patterns.get(j + 1);
                    String cs = segments.get(i);

                    if (matchSegment(cs, np)) {
                        break;
                    }

                    i++;
                }

                j++;
            } else if (matchSegment(segment, pattern)) {
                i++;
                j++;
            } else {
                return false;
            }
        }

        while (j < patterns.size() && ANY_MULTI_SEGMENT.equals(patterns.get(j))) {
            j++;
        }

        return patterns.size() == j && segments.size() == i;
    }

    /**
     * Matches a single segment against a pattern.
     *
     * @param segment the segment to evaluate
     * @param pattern the pattern to match against
     * @return {@code true} if the segment matches the pattern, {@code false} otherwise
     */
    public boolean matchSegment(String segment, String pattern) {
        // * matches any segment
        if (pattern.equals(ANY_SINGLE_SEGMENT)) {
            return true;
        }

        if (pattern.contains(ANY_SINGLE_SEGMENT)) {
            // Handle wildcard patterns like *.java
            String regex = pattern.replace(".", "\\.").replace("*", ".+");
            return segment.matches(regex);
        }

        if (pattern.contains(ANY_CHARACTER)) {
            // Ensure lengths match
            if (segment.length() != pattern.length()) {
                return false;
            }

            // Check char-by-char
            for (int i = 0; i < pattern.length(); i++) {
                if (pattern.charAt(i) != '?' && pattern.charAt(i) != segment.charAt(i)) {
                    return false;
                }
            }

            return true;
        }

        // Exact match
        return segment.equals(pattern);
    }

}
