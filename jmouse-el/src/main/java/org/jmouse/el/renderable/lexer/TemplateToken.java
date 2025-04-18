package org.jmouse.el.renderable.lexer;

import org.jmouse.el.lexer.Token;

/**
 * Enumeration of template tokens used in the template engine.
 * Each type has a unique type identifier and example string(s) associated with it.
 */
public enum TemplateToken implements Token.Type {

    // Print tokens
    /**
     * Opening print type: e.g., "{{"
     */
    T_OPEN_PRINT(10001, "{{"),

    /**
     * Closing print type: e.g., "}}"
     */
    T_CLOSE_PRINT(10002, "}}"),

    // Call tokens (used for control structures)
    /**
     * Opening call type: e.g., "{%"
     */
    T_OPEN_EXPRESSION(10003, "{%"),

    /**
     * Closing call type: e.g., "%}"
     */
    T_CLOSE_EXPRESSION(10004, "%}"),

    // Comment tokens
    /**
     * Opening comment type: e.g., "{#"
     */
    T_OPEN_COMMENT(10005, "{#"),

    /**
     * Closing comment type: e.g., "#}"
     */
    T_CLOSE_COMMENT(10006, "#}"),
    /**
     * Opening comment type: e.g., "{!"
     */
    T_OPEN_JAVA_CODE(10007, "{!"),

    /**
     * Closing comment type: e.g., "!}"
     */
    T_CLOSE_JAVA_CODE(10008, "!}"),

    // Keywords and control structures (grouped by 20000+)
    /**
     * 'for' keyword type.
     */
    T_FOR(20001, "for"),

    /**
     * 'endfor' keyword type.
     */
    T_END_FOR(20002, "endfor", "end-for"),

    /**
     * 'if' keyword type.
     */
    T_IF(20003, "if"),

    /**
     * 'endif' keyword type (also supports "fi" as an alternative).
     */
    T_END_IF(20004, "endif", "end-if", "eif", "fi"),

    /**
     * 'elseif' keyword type (also supports "else-if" as an alternative).
     */
    T_ELSE_IF(20004, "elseif", "else-if"),

    /**
     * 'else' keyword type.
     */
    T_ELSE(20005, "else"),

    /**
     * 'in' keyword type.
     */
    T_IN(20006, "in"),

    /**
     * 'as' keyword type.
     */
    T_AS(20007, "as"),

    /**
     * 'with' keyword type.
     */
    T_WITH(20008, "with"),

    /**
     * 'extends' keyword type.
     */
    T_EXTENDS(20009, "extends"),

    /**
     * 'block' keyword type.
     */
    T_BLOCK(20010, "block"),

    /**
     * 'block' keyword type.
     */
    T_END_BLOCK(20011, "endblock", "end_block"),

    /**
     * 'macro' keyword type.
     */
    T_MACRO(20012, "macro"),

    /**
     * 'macro' keyword type.
     */
    T_END_MACRO(20013, "endmacro", "end_macro"),

    // Additional tokens
    /**
     * 'include' keyword type.
     */
    T_INCLUDE(20014, "include"),

    /**
     * 'call' keyword type.
     */
    T_CALL(20015, "call"),

    /**
     * 'set' keyword type.
     */
    T_SET(20016, "set"),

    /**
     * 'endset' keyword type.
     */
    T_END_SET(20017, "endset"),

    /**
     * 'import' keyword type.
     */
    T_IMPORT(20018, "import"),

    /**
     * 'from' keyword type.
     */
    T_FROM(20019, "from"),

    /**
     * 'apply' keyword type.
     */
    T_APPLY(20020, "apply"),

    /**
     * 'endapply' keyword type.
     */
    T_END_APPLY(20021, "endapply", "end_apply"),

    /**
     * 'embed' keyword type.
     */
    T_EMBED(20021, "embed"),

    /**
     * 'endembed' keyword type.
     */
    T_END_EMBED(20022, "endembed", "end_embed"),

    /**
     * 'scope' keyword type.
     */
    T_SCOPE(20023, "scope"),

    /**
     * 'endscope' keyword type.
     */
    T_END_SCOPE(20023, "endscope", "end_scope"),

    /**
     * 'render' keyword type.
     */
    T_RENDER(20024, "render"),

    /**
     * 'do' keyword type.
     */
    T_DO(20025, "do"),

    /**
     * 'use' keyword type.
     */
    T_USE(20026, "use"),

    /**
     * 'get' keyword type.
     */
    T_GET(20027, "get"),

    /**
     * 'cache' keyword type.
     */
    T_CACHE(20028, "cache"),

    /**
     * 'endcache' keyword type.
     */
    T_END_CACHE(20029, "endcache", "end_cache"),

    /**
     * 'ignore_missing' keyword type.
     */
    T_IGNORE_MISSING(21000, "ignore_missing"),

    /**
     * 'override' keyword type.
     */
    T_OVERRIDE(21010, "override"),

    /**
     * raw piece of html or text
     */
    T_RAW_TEXT(30001);

    private final int      type;
    private final String[] values;

    /**
     * Constructs a template type with the specified type and example usages.
     *
     * @param type     the unique type identifier for this type
     * @param values example strings representing the type
     */
    TemplateToken(final int type, final String... values) {
        this.type = type;
        this.values = values;
    }

    @Override
    public int getTypeId() {
        return type;
    }

    @Override
    public <E extends Enum<E>> E getEnumType() {
        return (E) this;
    }

    @Override
    public <E extends Enum<E>> Class<E> getBundleType() {
        return (Class<E>) getEnumType().getClass();
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
