package org.jmouse.parallax.file;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A graph line that is a family: any line followed by the range of a whole index, as jMX writes an integer input —
 * {@code y = sin((2k - 1) x) / (2k - 1)  k in 1 .. N integer}. What comes before the range is read as it would be
 * alone (a curve, a parametric curve, a point, a shape) with the index as one of its parameters; the family is that
 * member once for every whole value of the index.
 */
public final class FamilyLines {

    private static final Pattern RANGE = Pattern.compile("(.*\\S)\\s+([A-Za-z][A-Za-z0-9_]*)\\s+in\\s+(.+?)\\s*\\.\\.\\s*(.+?)\\s+integer\\s*",
            Pattern.DOTALL);

    /** The member's text, the index and its bounds as written. */
    public record Split(String member, String index, String from, String to) {
    }

    private FamilyLines() {
    }

    /** The line without its range, and the range; empty when the line ends with no `name in a .. b integer`. */
    public static Optional<Split> split(String source) {
        int last = lastSeparator(source);
        String head = last < 0 ? "" : source.substring(0, last + 1);
        Matcher range = RANGE.matcher(source.substring(last + 1));
        if (!range.matches()) {
            return Optional.empty();
        }
        return Optional.of(new Split(head + range.group(1), range.group(2), range.group(3).strip(), range.group(4).strip()));
    }

    /** Where the last statement begins: after the last `;` outside parentheses. */
    private static int lastSeparator(String source) {
        int depth = 0;
        int last = -1;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '(' || character == '[') {
                depth++;
            } else if (character == ')' || character == ']') {
                depth--;
            } else if (character == ';' && depth == 0) {
                last = index;
            }
        }
        return last;
    }
}
