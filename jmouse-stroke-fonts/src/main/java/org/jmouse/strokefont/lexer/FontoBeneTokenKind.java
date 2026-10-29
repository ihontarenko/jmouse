package org.jmouse.strokefont.lexer;

import org.jmouse.el.lexer.Token;

/**
 * What a line of a FontoBene file is — the format is a line a statement, so a token is a line —: in the header a section,
 * an entry or the separator; after it a glyph's declaration, a reference to an earlier glyph, its trailing space, a
 * polyline, or the blank line that ends it; a comment anywhere.
 */
public enum FontoBeneTokenKind implements Token.Type {

    COMMENT(32000),
    SECTION(32010),
    ENTRY(32020),
    SEPARATOR(32030, "---"),
    GLYPH(32040),
    REFERENCE(32050),
    SPACING(32060),
    POLYLINE(32070),
    BLANK(32080),
    END_OF_LINE(32090),
    END_OF_TEXT(32999);

    private final int      typeId;
    private final String[] templates;

    FontoBeneTokenKind(int typeId, String... templates) {
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
        return (Class<E>) FontoBeneTokenKind.class;
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
