package org.jmouse.formula.el;

/**
 * The notation people type, brought to what jME's lexer reads: a superscript is a power (x² → x^2), the
 * typographic minus, dot, cross and obelus are -, *, * and /, and π is pi. jME itself stays ASCII; this is jMF's.
 */
public final class FormulaNotation {

    private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    private FormulaNotation() {
    }

    public static String normalize(String source) {
        StringBuilder normalized = new StringBuilder(source.length() + 8);
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            int digit = SUPERSCRIPTS.indexOf(character);
            if (digit >= 0) {
                normalized.append('^');
                while (index < source.length() && SUPERSCRIPTS.indexOf(source.charAt(index)) >= 0) {
                    normalized.append(SUPERSCRIPTS.indexOf(source.charAt(index)));
                    index++;
                }
                index--;
                continue;
            }
            switch (character) {
                case '−', '–' -> normalized.append('-');
                case '·', '×', '⋅' -> normalized.append('*');
                case '÷' -> normalized.append('/');
                case 'π' -> normalized.append(" pi ");
                case '∞' -> normalized.append(" inf ");
                case '≤' -> normalized.append("<=");
                case '≥' -> normalized.append(">=");
                case '≠' -> normalized.append("!=");
                case '∧' -> normalized.append(" and ");
                case '∨' -> normalized.append(" or ");
                case '¬' -> normalized.append('!');
                default -> normalized.append(character);
            }
        }
        return normalized.toString();
    }
}
