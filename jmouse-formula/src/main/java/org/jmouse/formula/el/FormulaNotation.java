package org.jmouse.formula.el;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * The notation people type, brought to what jME's lexer reads: a superscript is a power (x² → x^2), the
 * typographic minus, dot, cross and obelus are -, *, * and /, π is pi, and the relations and connectives are their
 * words. jME itself stays ASCII; this is jMF's — and a language whose expressions are jMF's reads it from here.
 */
public final class FormulaNotation {

    private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    /** Every symbol but the superscript digits, and the text it stands for. */
    private static final Map<Character, String> SYMBOLS = Map.ofEntries(
            Map.entry('−', "-"), Map.entry('–', "-"), Map.entry('·', "*"), Map.entry('×', "*"), Map.entry('⋅', "*"),
            Map.entry('÷', "/"), Map.entry('π', " pi "), Map.entry('∞', " inf "), Map.entry('≤', "<="),
            Map.entry('≥', ">="), Map.entry('≠', "!="), Map.entry('∧', " and "), Map.entry('∨', " or "),
            Map.entry('¬', "!"));

    /** The most characters one typed character becomes: a symbol's words, or a superscript's caret and digit. */
    private static final int LONGEST_WRITING = Math.max(2, SYMBOLS.values().stream().mapToInt(String::length).max().orElse(1));

    private FormulaNotation() {
    }

    public static String normalize(String source) {
        return normalizeTracked(source).text();
    }

    /**
     * A text brought to what the lexer reads, with where each of its characters came from — what lets a position
     * the lexer found be pointed at in the text as it was typed.
     *
     * @param text   the normalized text
     * @param origin for every character of {@code text}, the index of the source character it was written as; one more
     *               entry, the source's length, stands for the end
     */
    public record Normalized(String text, int[] origin) {

        /** Where a position of the normalized text stands in the source: an index from 0 to the source's length. */
        public int sourceIndex(int normalizedIndex) {
            return origin[Math.max(0, Math.min(normalizedIndex, origin.length - 1))];
        }
    }

    /** {@link #normalize}, keeping where each normalized character came from. */
    public static Normalized normalizeTracked(String source) {
        StringBuilder normalized = new StringBuilder(source.length() + 8);
        int[] origin = new int[source.length() * LONGEST_WRITING + 1];
        boolean afterSuperscript = false;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            int digit = superscriptDigit(character);
            String written;
            if (digit >= 0) {
                written = (afterSuperscript ? "" : "^") + digit;
                afterSuperscript = true;
            } else {
                afterSuperscript = false;
                written = symbol(character).orElse(String.valueOf(character));
            }
            for (int offset = 0; offset < written.length(); offset++) {
                origin[normalized.length() + offset] = index;
            }
            normalized.append(written);
        }
        origin[normalized.length()] = source.length();
        return new Normalized(normalized.toString(), Arrays.copyOf(origin, normalized.length() + 1));
    }

    /**
     * What a symbol of the notation stands for — − is {@code -}, ≤ is {@code <=}, ∧ is {@code and} —; empty for a
     * character that is not one. A superscript digit is not answered here: a run of them is one power
     * ({@link #superscriptDigit}).
     */
    public static Optional<String> symbol(char character) {
        return Optional.ofNullable(SYMBOLS.get(character));
    }

    /** The digit a superscript writes — ² is 2 —; -1 for any other character. A run of them is one power: x²³ is x^23. */
    public static int superscriptDigit(char character) {
        return SUPERSCRIPTS.indexOf(character);
    }
}
