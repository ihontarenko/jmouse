package org.jmouse.svg.path.lexer;

import org.jmouse.el.lexer.Token;

/**
 * The jME {@link Token.Type token types} of SVG path data: a command's letter ({@code M}, {@code h}, {@code A}…) and a
 * number. Whitespace and commas only separate; they are no tokens.
 */
public enum PathTokenKind implements Token.Type {

    /** One letter: the command it opens. */
    COMMAND(33000),
    /** A number, its sign and exponent and all: {@code -4}, {@code .5}, {@code 1e-3}. */
    NUMBER(33010),
    UNKNOWN(33900),
    /** The end of the text; always the last token. */
    END_OF_TEXT(33999);

    private final int typeId;

    PathTokenKind(int typeId) {
        this.typeId = typeId;
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
        return (Class<E>) PathTokenKind.class;
    }

    @Override
    public String[] getTokenTemplates() {
        return new String[0];
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }

    public boolean matches(Token token) {
        return token != null && token.type() == this;
    }
}
