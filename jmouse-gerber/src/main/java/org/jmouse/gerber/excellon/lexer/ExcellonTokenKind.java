package org.jmouse.gerber.excellon.lexer;

import org.jmouse.el.lexer.Token;

/**
 * The jME {@link Token.Type token types} of Excellon: a language of lines, each a statement, made of words — a letter
 * group and its number ({@code T1}, {@code C0.8}, {@code X12.5}), a keyword ({@code METRIC}), a bare number.
 */
public enum ExcellonTokenKind implements Token.Type {

    WORD(31000),

    /** {@code ,}: between a keyword and its values — {@code FMAT,2}, {@code METRIC,TZ}. */
    COMMA(31010, ","),

    /** {@code %}: the end of the header. */
    PERCENT(31020, "%"),

    /** {@code ; …}: a comment line, its text without the semicolon. */
    COMMENT(31030),

    /** Where a statement ends: the end of its line. */
    END_OF_LINE(31040),

    UNKNOWN(31900),

    /** The end of the text; always the last token. */
    END_OF_TEXT(31999);

    private final int      typeId;
    private final String[] templates;

    ExcellonTokenKind(int typeId, String... templates) {
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
        return (Class<E>) ExcellonTokenKind.class;
    }

    @Override
    public String[] getTokenTemplates() {
        return templates.clone();
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }

    public boolean matches(Token token) {
        return token != null && token.type() == this;
    }
}
