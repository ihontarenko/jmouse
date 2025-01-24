package svit.matcher.ant;

import svit.matcher.Matcher;

import java.util.*;


final public class AntMatcher implements Matcher<String> {

    public static final String ANY_CHARACTER      = "?"; // Matches exactly one character
    public static final String ANY_SINGLE_SEGMENT = "*"; // Matches zero or more characters in a single segment
    public static final String ANY_MULTI_SEGMENT  = "**"; // Matches zero or more segments (including empty)

    private final Collection<String> patterns = new ArrayList<>();
    private final String             token;

    /**
     * Constructs an AntMatcher with the specified pattern and token.
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
     * Constructs an AntMatcher with the specified pattern.
     * The default token is "/".
     *
     * @param pattern the Ant-style pattern to use
     * @throws NullPointerException if the pattern is null
     */
    public AntMatcher(String pattern) {
        this(pattern, "/");
    }

    /**
     * Matches the given string against the Ant-style pattern.
     *
     * <p>
     * This method splits both the input string and the pattern into segments using the specified token
     * (default is "/"). It then compares the segments recursively to determine if the input string matches
     * the pattern.
     * </p>
     *
     * <h3>Steps:</h3>
     * <ol>
     *   <li>Splits the input string into segments based on the token.</li>
     *   <li>Creates a modifiable copy of the pattern segments for processing.</li>
     *   <li>Checks if the input string has fewer segments than the pattern. If so, returns {@code false}.</li>
     *   <li>Removes any empty segments from both the input string and the pattern.</li>
     *   <li>Starts the matching process from the beginning of both lists, using a recursive helper method.</li>
     * </ol>
     *
     * @param item the input string to match against the pattern
     * @return {@code true} if the input string matches the pattern; {@code false} otherwise
     */
    public boolean matches(String item) {
        // Step 1: Split the input string into segments using the token
        List<String> segments = new ArrayList<>(List.of(item.split(token)));
        // Step 2: Create a modifiable copy of the pattern segments
        List<String> patterns = new ArrayList<>(this.patterns);

        // Step 3: If there are more segments in the string than in the pattern, return false
        if (segments.size() < patterns.size()) {
            return false;
        }

        // Step 4: Remove any empty segments from both lists
        segments.removeIf(String::isBlank);
        patterns.removeIf(String::isBlank);

        // Step 5: Start matching from the beginning of both lists
        int i = 0, j = 0;

        // Call the recursive method to perform the actual matching
        return matches(segments, patterns, i, j);
    }

    /**
     * Matches segments of a string against a pattern using Ant-style matching.
     *
     * @param segments the segments of the string to be matched (e.g., path segments)
     * @param patterns the segments of the pattern to match against
     * @param i the current index of the segments list
     * @param j the current index of the patterns list
     * @return true if the segments match the pattern, false otherwise
     */
    public boolean matches(List<String> segments, List<String> patterns, int i, int j) {
        // Step 1: If both the pattern and the string have been fully matched, return true.
        if (i == segments.size() && j == patterns.size()) {
            return true; // Both lists are fully processed, meaning they match.
        }

        // Step 2: If the pattern is fully processed but the string still has segments, return false.
        if (j == patterns.size()) {
            return false; // Pattern is exhausted, but segments remain unmatched.
        }

        // Step 3: If the segments are fully processed but the pattern still has segments, ensure the remaining pattern contains only `**`.
        if (i == segments.size()) {
            // Skip all remaining `**` patterns, as they match any sequence of segments (including empty).
            while (j < patterns.size() && patterns.get(j).equals(ANY_MULTI_SEGMENT)) {
                j++;
            }
            // If the remaining pattern contains anything other than `**`, it's not a match.
            return j == patterns.size();
        }

        // Step 4: Get the current segment from the string and the current pattern to match against.
        String segment = segments.get(i);
        String pattern = patterns.get(j);

        // Step 5: If the current pattern is `**`, it can match any sequence of segments.
        if (pattern.equals(ANY_MULTI_SEGMENT)) {
            // Try two possible matches:
            // 1. Skip the current segment in the string but keep the `**` pattern (i + 1, j).
            // 2. Skip the `**` pattern and try to match the current segment with the next pattern (i, j + 1).
            return matches(segments, patterns, i + 1, j) || matches(segments, patterns, i, j + 1);
        }

        // Step 6: If the current segment matches the current pattern, continue to the next segment and pattern.
        if (matches(segment, pattern)) {
            return matches(segments, patterns, i + 1, j + 1);
        }

        // Step 7: If none of the above conditions apply, the segments and pattern do not match.
        return false;
    }

    /**
     * Matches a single segment against a pattern.
     *
     * @param segment the segment to evaluate
     * @param pattern the pattern to match against
     * @return true if the segment matches the pattern, false otherwise
     */
    public boolean matches(String segment, String pattern) {
        // Step 1: If the pattern is *, it matches any segment
        if (pattern.equals(ANY_SINGLE_SEGMENT)) {
            return true;
        }

        // Step 2: If the pattern contains *, handle it as a wildcard for multiple characters
        if (pattern.contains(ANY_SINGLE_SEGMENT)) {
            // Step 1: Find the position of the first '*' in the pattern
            int starIndex = pattern.indexOf(ANY_SINGLE_SEGMENT);

            // Step 2: Separate the pattern into a prefix and suffix based on the position of '*'
            // The prefix is the part of the pattern before '*'
            String prefix = pattern.substring(0, starIndex);
            // The suffix is the part of the pattern after '*'
            String suffix = pattern.substring(starIndex + 1);

            // Ensure that only one '*' character is present in the pattern
            // If there is more than one '*', this logic might not work correctly
            if (pattern.indexOf(ANY_SINGLE_SEGMENT, starIndex + 1) != -1) {
                return false;
            }

            // Step 3: Check if the segment starts with the prefix and ends with the suffix
            // This ensures that the part of the segment between the prefix and suffix matches '*'
            return segment.startsWith(prefix) && segment.endsWith(suffix);
        }

        // Step 3: If the pattern contains ?, handle it as a wildcard for a single character
        if (pattern.contains(ANY_CHARACTER)) {
            // Ensure lengths match
            if (segment.length() != pattern.length()) {
                return false;
            }

            // Check each character in the segment and pattern
            for (int i = 0; i < pattern.length(); i++) {
                if (pattern.charAt(i) != '?' && pattern.charAt(i) != segment.charAt(i)) {
                    return false;
                }
            }

            return true; // All characters matched
        }

        // Step 4: If no special characters, match the segment exactly
        return segment.equals(pattern);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(token, "AntMatcher[ ", " ]");
        patterns.forEach(joiner::add);
        return joiner.toString();
    }
}
