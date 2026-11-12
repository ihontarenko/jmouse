package org.jmouse.el.lexer;

import org.jmouse.core.text.Slugifier;

import java.util.regex.Pattern;

/**
 * What the language reads as an identifier — a letter or an underscore, then letters, digits and underscores — and the
 * identifier a text written elsewhere becomes: in Latin letters, every run of what an identifier cannot hold one
 * underscore. The one place the rule is spelled; the lexer reads by it.
 */
public final class Identifiers {

    /** The characters an identifier may open with. */
    private static final String OPENING = "a-zA-Z_";

    /** The characters it may go on with. */
    private static final String FOLLOWING = "a-zA-Z0-9_";

    /** A character an identifier may hold, as a regular expression: what may not follow a word that ends one. */
    public static final String CHARACTER = "[" + FOLLOWING + "]";

    /** An identifier, as a regular expression the lexer composes into its own. */
    public static final String PATTERN = "[" + OPENING + "]" + CHARACTER + "*";

    private static final Pattern IDENTIFIER = Pattern.compile(PATTERN);
    private static final Pattern NOT_HELD = Pattern.compile("[^" + FOLLOWING + "]+");
    private static final Pattern OPENS = Pattern.compile("^[" + OPENING + "]");
    private static final Pattern EDGES = Pattern.compile("^_+|_+$");
    private static final char JOINT = '_';

    private Identifiers() {
    }

    public static boolean isIdentifier(String text) {
        return text != null && IDENTIFIER.matcher(text).matches();
    }

    /**
     * The identifier a text becomes: {@code GND@1} is {@code GND_1}, «Вхід» is {@code Vkhid}; one that would open with
     * what an identifier cannot — {@code 1}, {@code 3V3} — opens with {@code opening} instead ({@code pin1}), and so does
     * one of which nothing is left.
     *
     * @param opening an identifier's beginning, written before a text that cannot begin one
     */
    public static String of(String text, String opening) {
        String held = EDGES.matcher(NOT_HELD.matcher(Slugifier.standard().latin(text)).replaceAll(String.valueOf(JOINT)))
                .replaceAll("");
        return OPENS.matcher(held).find() ? held : opening + held;
    }
}
