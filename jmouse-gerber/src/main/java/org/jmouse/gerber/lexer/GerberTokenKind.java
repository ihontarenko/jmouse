package org.jmouse.gerber.lexer;

import org.jmouse.el.lexer.Token;

/**
 * The jME {@link Token.Type token types} of Gerber.
 *
 * <p>Gerber is a language of blocks, each ended by {@code *}; an extended command is one or more blocks between two
 * {@code %}. Both boundaries are tokens here — {@link #END_OF_BLOCK}, {@link #PERCENT} — so the parsers read commands
 * off one {@link org.jmouse.el.lexer.TokenCursor} the way any jME parser reads its tokens.</p>
 */
public enum GerberTokenKind implements Token.Type {

    /** {@code %}: an extended command opens or closes. */
    PERCENT(30000, "%"),

    /** A word of a word command: a letter and its number — {@code X-1500}, {@code D01}, {@code G75}, {@code M02}. */
    WORD(30010),

    /** The body of an extended command's block as written — {@code ADD10C,0.25} —, or a comment's text after {@code G04}. */
    DATA(30020),

    /** {@code *}: where a block ends. */
    END_OF_BLOCK(30030, "*"),

    /** A character that starts nothing Gerber has. */
    UNKNOWN(30900),

    /** The end of the text; always the last token. */
    END_OF_TEXT(30999);

    private final int      typeId;
    private final String[] templates;

    GerberTokenKind(int typeId, String... templates) {
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
        return (Class<E>) GerberTokenKind.class;
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
}
