package org.jmouse.template.lexer;

/**
 * Enumeration of template tokens used in the template engine.
 * Each token has a unique type identifier and example string(s) associated with it.
 */
public enum TemplateToken implements Token {

    // Print tokens
    /**
     * Opening print token: e.g., "{{"
     */
    T_OPEN_PRINT(10001, "{{"),

    /**
     * Closing print token: e.g., "}}"
     */
    T_CLOSE_PRINT(10002, "}}"),

    // Call tokens (used for control structures)
    /**
     * Opening call token: e.g., "{%"
     */
    T_OPEN_CALL(10003, "{%"),

    /**
     * Closing call token: e.g., "%}"
     */
    T_CLOSE_CALL(10004, "%}"),

    // Comment tokens
    /**
     * Opening comment token: e.g., "{#"
     */
    T_OPEN_COMMENT(10005, "{#"),

    /**
     * Closing comment token: e.g., "#}"
     */
    T_CLOSE_COMMENT(10006, "#}"),

    // Keywords and control structures (grouped by 20000+)
    /**
     * 'for' keyword token.
     */
    T_FOR(20001, "for"),

    /**
     * 'endfor' keyword token.
     */
    T_END_FOR(20002, "endfor"),

    /**
     * 'if' keyword token.
     */
    T_IF(20003, "if"),

    /**
     * 'endif' keyword token (also supports "fi" as an alternative).
     */
    T_END_IF(20004, "endif", "fi"),

    /**
     * 'else' keyword token.
     */
    T_ELSE(20005, "else"),

    /**
     * 'in' keyword token.
     */
    T_IN(20006, "in"),

    /**
     * 'with' keyword token.
     */
    T_WITH(20007, "with"),

    /**
     * 'extends' keyword token.
     */
    T_EXTENDS(20008, "extends"),

    /**
     * 'block' keyword token.
     */
    T_BLOCK(20009, "block"),

    /**
     * 'macro' keyword token.
     */
    T_MACRO(20010, "macro"),

    // Additional tokens
    /**
     * 'include' keyword token.
     */
    T_INCLUDE(20011, "include"),

    /**
     * 'call' keyword token.
     */
    T_CALL(20012, "call"),

    /**
     * 'set' keyword token.
     */
    T_SET(20013, "set"),

    /**
     * 'endset' keyword token.
     */
    T_END_SET(20014, "endset"),

    /**
     * 'import' keyword token.
     */
    T_IMPORT(20015, "import"),

    /**
     * raw piece of html or text
     */
    T_RAW_TEXT(30001);

    private final int type;
    private final String[] examples;

    /**
     * Constructs a template token with the specified type and example usages.
     *
     * @param type     the unique type identifier for this token
     * @param examples example strings representing the token
     */
    TemplateToken(final int type, final String... examples) {
        this.type = type;
        this.examples = examples;
    }

    /**
     * Returns the token type identifier.
     *
     * @return the token type identifier
     */
    @Override
    public int type() {
        return type;
    }

    /**
     * Returns example usages of this token.
     *
     * @return an array of example strings
     */
    @Override
    public String[] examples() {
        return examples;
    }

    /**
     * Returns all tokens as an array.
     *
     * @return an array of all TemplateToken values
     */
    @Override
    public Token[] tokens() {
        return values();
    }

}
