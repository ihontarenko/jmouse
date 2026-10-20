package org.jmouse.formula.el;

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

    private FormulaNotation() {
    }

    public static String normalize(String source) {
        StringBuilder normalized = new StringBuilder(source.length() + 8);
        boolean afterSuperscript = false;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            int digit = superscriptDigit(character);
            if (digit >= 0) {
                normalized.append(afterSuperscript ? "" : "^").append(digit);
                afterSuperscript = true;
                continue;
            }
            afterSuperscript = false;
            normalized.append(symbol(character).orElse(String.valueOf(character)));
        }
        return normalized.toString();
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
