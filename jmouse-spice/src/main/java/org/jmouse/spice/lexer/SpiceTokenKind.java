package org.jmouse.spice.lexer;

import org.jmouse.el.lexer.Token;

import java.util.Locale;

/**
 * The jME {@link Token.Type token types} of SPICE.
 *
 * <p>SPICE is a language of lines: a card ends where its line ends, unless the next line begins with {@code +}. That
 * boundary is a token here — {@link #END_OF_CARD} — so that the parsers read cards off one {@link
 * org.jmouse.el.lexer.TokenCursor} the way any jME parser reads its tokens, with no second notion of a line.</p>
 *
 * <p>Keywords are words: {@code .model}, {@code DC} or {@code SIN} are {@link #WORD}s told apart by their value,
 * without regard to case, as SPICE tells them apart.</p>
 */
public enum SpiceTokenKind implements Token.Type {

    /** The first line of a deck: its title, whatever it says. */
    TITLE(20000),

    /** A name, a node, a keyword, a dot card's word, or a number with its scale factor: {@code 10uF}, {@code 1meg}. */
    WORD(20010),

    /** {@code {2*R1}} or {@code '2*R1'}: an expression, its delimiters kept in the value. */
    EXPRESSION(20020),

    /** {@code "models.lib"}: a text in double quotes, its quotes kept in the value. */
    TEXT(20030),

    /** {@code (} */
    OPEN(20040, "("),

    /** {@code )} */
    CLOSE(20050, ")"),

    /** {@code ,} */
    COMMA(20060, ","),

    /** {@code =} */
    EQUALS(20070, "="),

    /** A line that is a comment: {@code * …}, or {@code ;} / {@code $} at the start of a line. */
    COMMENT(20080),

    /** A comment after a card on its line: {@code ; …}, or {@code $ …} after a blank. */
    TRAILING_COMMENT(20090),

    /** One line of a {@code .control} … {@code .endc} script, as written: SPICE's interpreter's, not the netlist's. */
    CONTROL_LINE(20100),

    /** Where a card ends: the end of its last line. */
    END_OF_CARD(20110),

    /** A character that starts nothing SPICE has. */
    UNKNOWN(20900),

    /** The end of the text; always the last token. */
    END_OF_TEXT(20999);

    private final int      typeId;
    private final String[] templates;

    SpiceTokenKind(int typeId, String... templates) {
        this.typeId = typeId;
        this.templates = templates;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) SpiceTokenKind.class;
    }

    @Override
    public String[] getTokenTemplates() {
        return templates.clone();
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }

    /** Whether a token is of this kind. */
    public boolean matches(Token token) {
        return token != null && token.type() == this;
    }

    /** Whether a token is the word given, compared as SPICE compares words: without regard to case. */
    public static boolean isWord(Token token, String word) {
        return WORD.matches(token) && token.value().equalsIgnoreCase(word);
    }

    /** A word's value in lower case: how SPICE's keywords are compared. */
    public static String lower(Token token) {
        return token.value().toLowerCase(Locale.ROOT);
    }
}
