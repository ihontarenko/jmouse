package org.jmouse.java.parser.lexer;

import java.util.Arrays;
import java.util.List;
import org.jmouse.java.parser.source.LineIndex;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.ProblemKind;

/**
 * The first lexical translation of the Java language: Unicode escapes become the characters they name,
 * before anything else is recognised (JLS §3.3).
 *
 * <p>⚠️ This step is what makes the escape for a quote a real quote, and the escape for an asterisk
 * followed by a slash a real comment end — a lexer that skips it can be walked out of a string by an
 * escape. Offsets of the translated text are mapped back, so every span still points into the original.
 *
 * <p>A backslash begins an escape only when an even number of contiguous raw backslashes precedes it, so
 * a doubled backslash before {@code u0041} stays six characters.
 *
 * <p>⚠️ The escapes are spelled out in words in this comment on purpose: javac translates them inside
 * comments too, and the escape for an asterisk and a slash would end this one.
 */
final class UnicodeEscapeTranslation {

    private final String text;
    private final int[]  originalOffsets;

    private UnicodeEscapeTranslation(String text, int[] originalOffsets) {
        this.text = text;
        this.originalOffsets = originalOffsets;
    }

    /**
     * Translates every eligible escape.
     *
     * @param original the text as written
     * @param lines    its line index, for problem spans
     * @param problems where malformed escapes are reported
     * @return the translation; the identity, without a mapping table, when the text holds no escape
     */
    static UnicodeEscapeTranslation translate(CharSequence original, LineIndex lines, List<ParseProblem> problems) {
        int length = original.length();
        if (!containsEscapeCandidate(original)) {
            return new UnicodeEscapeTranslation(original.toString(), null);
        }

        StringBuilder translated     = new StringBuilder(length);
        int[]         offsets        = new int[length + 1];
        int           count          = 0;
        int           backslashCount = 0;
        int           position       = 0;

        while (position < length) {
            char character = original.charAt(position);
            if (character == '\\' && backslashCount % 2 == 0 && position + 1 < length && original.charAt(position + 1) == 'u') {
                int digitsStart = position + 1;
                while (digitsStart < length && original.charAt(digitsStart) == 'u') {
                    digitsStart++;
                }
                int value = hexadecimalValue(original, digitsStart);
                if (value >= 0) {
                    translated.append((char) value);
                    offsets[count++] = position;
                    position = digitsStart + 4;
                    backslashCount = 0;
                    continue;
                }
                problems.add(new ParseProblem(ProblemKind.INVALID_UNICODE_ESCAPE,
                        "A Unicode escape needs four hexadecimal digits.", lines.span(position, digitsStart)));
            }
            translated.append(character);
            offsets[count++] = position;
            backslashCount = character == '\\' ? backslashCount + 1 : 0;
            position++;
        }

        offsets[count] = length;
        return new UnicodeEscapeTranslation(translated.toString(), Arrays.copyOf(offsets, count + 1));
    }

    /**
     * Returns the translated text the lexer reads.
     *
     * @return the text with escapes replaced
     */
    String text() {
        return text;
    }

    /**
     * Maps an offset of the translated text back to the original.
     *
     * @param translatedOffset offset in {@link #text()}, up to its length inclusive
     * @return the offset in the original text
     */
    int originalOffset(int translatedOffset) {
        return originalOffsets == null ? translatedOffset : originalOffsets[translatedOffset];
    }

    private static boolean containsEscapeCandidate(CharSequence original) {
        for (int index = 0; index + 1 < original.length(); index++) {
            if (original.charAt(index) == '\\' && original.charAt(index + 1) == 'u') {
                return true;
            }
        }
        return false;
    }

    private static int hexadecimalValue(CharSequence original, int start) {
        if (start + 4 > original.length()) {
            return -1;
        }
        int value = 0;
        for (int index = start; index < start + 4; index++) {
            int digit = Character.digit(original.charAt(index), 16);
            if (digit < 0) {
                return -1;
            }
            value = value * 16 + digit;
        }
        return value;
    }
}
