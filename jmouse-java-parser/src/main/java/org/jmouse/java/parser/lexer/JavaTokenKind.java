package org.jmouse.java.parser.lexer;

import org.jmouse.el.lexer.Token;

/**
 * The jME {@link Token.Type token types} of Java's structural grammar.
 *
 * <p>⚠️ Operators are deliberately NOT combined: {@code >>}, {@code >=} and {@code ->} arrive as
 * separate single-character tokens. A declaration never needs an operator's meaning, while a combined
 * {@code >>} would have to be split again to close two levels of type arguments — the classic way a
 * Java lexer gets generics wrong.
 *
 * <p>The templates are the fixed spellings of punctuation; word kinds have none, because keywords are
 * told apart by their {@link Token#value() value}.
 */
public enum JavaTokenKind implements Token.Type {

    /** A name, including the contextual keywords {@code record}, {@code sealed}, {@code permits}, {@code var}, {@code yield}. */
    IDENTIFIER(10000),

    /** A reserved keyword such as {@code class} or {@code public}. */
    KEYWORD(10010),

    /** {@code true} or {@code false}. */
    BOOLEAN_LITERAL(10020, "true", "false"),

    /** {@code null}. */
    NULL_LITERAL(10030, "null"),

    /** An integer or floating-point literal in any radix. */
    NUMBER_LITERAL(10040),

    /** A single-line string literal. */
    STRING_LITERAL(10050),

    /** A {@code """} text block. */
    TEXT_BLOCK(10060),

    /** A character literal. */
    CHARACTER_LITERAL(10070),

    /** {@code (} */
    LEFT_PARENTHESIS(10100, "("),

    /** {@code )} */
    RIGHT_PARENTHESIS(10110, ")"),

    /** An opening brace. */
    LEFT_BRACE(10120, "{"),

    /** A closing brace. */
    RIGHT_BRACE(10130, "}"),

    /** {@code [} */
    LEFT_BRACKET(10140, "["),

    /** {@code ]} */
    RIGHT_BRACKET(10150, "]"),

    /** {@code ;} */
    SEMICOLON(10160, ";"),

    /** {@code ,} */
    COMMA(10170, ","),

    /** {@code .} */
    DOT(10180, "."),

    /** {@code ...} */
    ELLIPSIS(10190, "..."),

    /** {@code @} */
    AT(10200, "@"),

    /** {@code ::} */
    DOUBLE_COLON(10210, "::"),

    /** {@code <} */
    LESS(10220, "<"),

    /** {@code >} */
    GREATER(10230, ">"),

    /** {@code ?} */
    QUESTION(10240, "?"),

    /** {@code &} */
    AMPERSAND(10250, "&"),

    /** {@code =} */
    ASSIGN(10260, "="),

    /** Any other single operator character. */
    OPERATOR(10270, "+", "-", "*", "/", "%", "^", "|", "!", "~", ":"),

    /** A character that starts no token; reported as a problem. */
    UNKNOWN(10900),

    /** The end of the text; always the last token, with an empty value. */
    END_OF_FILE(10999);

    private final int      typeId;
    private final String[] templates;

    JavaTokenKind(int typeId, String... templates) {
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
        return (Class<E>) JavaTokenKind.class;
    }

    @Override
    public String[] getTokenTemplates() {
        return templates.clone();
    }

    @Override
    public Token.Type[] getTokens() {
        return values();
    }

    /**
     * Returns whether a token has this kind.
     *
     * @param token the token, possibly {@code null}
     * @return {@code true} when its type is this kind
     */
    public boolean matches(Token token) {
        return token != null && token.type() == this;
    }

    /**
     * Returns whether a token is a given reserved keyword.
     *
     * @param token   the token
     * @param keyword the keyword spelling
     * @return {@code true} when it is that keyword
     */
    public static boolean isKeyword(Token token, String keyword) {
        return KEYWORD.matches(token) && token.value().equals(keyword);
    }

    /**
     * Returns whether a token is an identifier with a given spelling — how contextual keywords are tested.
     *
     * @param token the token
     * @param name  the spelling
     * @return {@code true} when it is that identifier
     */
    public static boolean isIdentifier(Token token, String name) {
        return IDENTIFIER.matches(token) && token.value().equals(name);
    }
}
