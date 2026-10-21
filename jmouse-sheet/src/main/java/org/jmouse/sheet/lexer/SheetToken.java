package org.jmouse.sheet.lexer;

import org.jmouse.el.lexer.Token;

/**
 * The words sheet gives a meaning of its own.
 *
 * <p>⚠️ None of them is reserved. A field may be called {@code map} or {@code source} and a value may be the word
 * {@code as}: a name and a value are read as the words they are written with, whatever kind the lexer gave their
 * tokens. These kinds mark where a word is the language's — the head of a document, a citation — so that only those
 * places read it so.</p>
 */
public enum SheetToken implements Token.Type {

    /** {@code sheet "Title" as slug { … }} — the document, one per file. */
    T_SHEET(10000, "sheet"),

    /** {@code as slug} — the name a document is known by. */
    T_AS(10100, "as"),

    /** {@code source page 7 table 6.3} — where in the original a value or an entry was read. */
    T_SOURCE(10200, "source");

    private final int      type;
    private final String[] values;

    SheetToken(final int type, final String... values) {
        this.type = type;
        this.values = values;
    }

    @Override
    public int getTypeId() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) getDeclaringClass();
    }

    @Override
    public String[] getTokenTemplates() {
        return values;
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }
}
