package org.jmouse.kicad.parser.lexer;

import org.jmouse.el.lexer.Token;

/**
 * The jME {@link Token.Type token types} of a KiCad file.
 *
 * <p>KiCad writes every file as S-expressions: lists in parentheses, of atoms and texts. An atom is anything unquoted
 * — a keyword ({@code symbol}, {@code yes}), a number ({@code 1.27}, {@code -90}), a UUID as KiCad 7 wrote it —; a
 * text is in double quotes. The two are kept apart because KiCad keeps them apart: version 7 writes {@code (generator
 * eeschema)} and version 8 {@code (generator "eeschema")}, and a file is given back as it was written.</p>
 */
public enum KiCadTokenKind implements Token.Type {

    /** {@code (} */
    OPEN(30000, "("),

    /** {@code )} */
    CLOSE(30010, ")"),

    /** Anything unquoted: a keyword, a number, a bare identifier. */
    ATOM(30020),

    /** {@code "…"}: a text, its quotes and escapes kept in the value as written. */
    STRING(30030),

    /** A character that starts nothing a KiCad file has. */
    UNKNOWN(30900),

    /** The end of the text; always the last token. */
    END_OF_TEXT(30999);

    private final int      typeId;
    private final String[] templates;

    KiCadTokenKind(int typeId, String... templates) {
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
        return (Class<E>) KiCadTokenKind.class;
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
