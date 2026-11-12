package org.jmouse.mesh.vrml;

import org.jmouse.el.lexer.Token;

/**
 * The jME {@link Token.Type token types} of a VRML97 file: braces around a node's fields, brackets around a list, and
 * words — a node's type, a field's name, a number — and texts in quotes between them.
 */
public enum VrmlTokenKind implements Token.Type {

    /** An opening brace. */
    OPEN_BRACE(31000, "{"),

    /** A closing brace. */
    CLOSE_BRACE(31010, "}"),

    /** An opening bracket. */
    OPEN_BRACKET(31020, "["),

    /** A closing bracket. */
    CLOSE_BRACKET(31030, "]"),

    /** Anything unquoted: a node type, a field name, a number, {@code TRUE}. */
    WORD(31040),

    /** A text in double quotes, its quotes kept. */
    STRING(31050),

    /** A text opened and never closed. */
    UNKNOWN(31900),

    /** The end of the text; always the last token. */
    END_OF_TEXT(31999);

    private final int      typeId;
    private final String[] templates;

    VrmlTokenKind(int typeId, String... templates) {
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
        return (Class<E>) VrmlTokenKind.class;
    }

    @Override
    public String[] getTokenTemplates() {
        return templates.clone();
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }
}
