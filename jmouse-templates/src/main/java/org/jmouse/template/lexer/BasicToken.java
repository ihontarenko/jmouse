package org.jmouse.template.lexer;

/**
 * BasicToken defines token types for the template language.
 * 
 * <p>Tokens are grouped by category using numeric ranges:
 * <br>
 * Group 1000: Comparison operators<br>
 * Group 6000: Assignment operators<br>
 * Group 2000: Arithmetic operators and punctuation<br>
 * Group 3000: String literals<br>
 * Group 4000: Numeric literals<br>
 * Group 7000: Identifiers<br>
 * Group 9000: Keywords and constants
 * </p>
 *
 * <p>Each token type is assigned a unique integer id and a set of literal representations.</p>
 *
 * @author ...
 */
public enum BasicToken implements Token.Type {

    // Control tokens
    T_UNKNOWN(1),
    T_EOL(-1),
    T_SOL(-1),

    // Comparison operators (Group 1000)
    T_EQ(1010, "=", "==", "EQ"),
    T_NE(1020, "!=", "<>", "NOT", "NE", "NEQ"),
    T_GT(1030, ">", "GT"),
    T_GE(1040, ">=", "GTE", "GE"),
    T_LT(1050, "<", "LT"),
    T_LE(1060, "<=", "LTE", "LE"),
    T_AND(1070, "&&", "and"),
    T_OR(1080, "||", "or"),

    // Assignment operators (Group 6000)
    T_COLON_ASSIGN(6110, ":="),
    T_PLUS_ASSIGN(6120, "+="),
    T_MINUS_ASSIGN(6130, "-="),
    T_MULTIPLY_ASSIGN(6140, "*="),
    T_DIVIDE_ASSIGN(6150, "/="),

    // Arithmetic operators and punctuation (Group 2000)
    T_PLUS(2030, "+"),
    T_MINUS(2020, "-"),
    T_MULTIPLY(2050, "*"),
    T_DIVIDE(2040, "/"),
    T_PERCENT(2210, "%"),
    T_CARET(2220, "^"),

    T_AT(2000, "@"),
    T_HASH(2010, "#"),
    T_DOT(2060, "."),
    T_COMMA(2070, ","),
    T_NEGATE(2080, "!"),
    T_QUESTION(2090, "?"),
    T_BACKSLASH(2100, "\\"),  // Звичайна зворотна коса риска
    T_VERTICAL_SLASH(2110, "|"),
    T_AMPERSAND(2120, "&"),
    T_COLON(2130, ":"),
    T_SEMICOLON(2140, ";"),
    T_GRAVE_ACCENT(2150, "`"),
    T_OPEN_PAREN(2160, "("),
    T_CLOSE_PAREN(2180, ")"),
    T_OPEN_CURLY(2170, "{"),
    T_CLOSE_CURLY(2190, "}"),
    T_TILDA(2200, "~"),
    T_DOLLAR(2230, "$"),
    T_OPEN_BRACKET(2240, "["),
    T_CLOSE_BRACKET(2250, "]"),

    // Literals (Group 3000 for strings, Group 4000 for numbers)
    T_STRING(3000),
    T_INT(4000),
    T_FLOAT(5000),

    // Keywords and constants (Group 9000)
    T_FALSE(9000, "FALSE"),
    T_TRUE(9001, "TRUE"),
    T_NULL(9999, "NULL"),
    T_IF(9010, "if"),
    T_ELSE(9011, "else"),
    T_ENDIF(9012, "endif"),
    T_FOR(9020, "for"),
    T_ENDFOR(9021, "endfor"),
    T_WHILE(9030, "while"),
    T_ENDWHILE(9031, "endwhile"),

    // Identifier (Group 7000)
    T_IDENTIFIER(7000);

    private final int type;
    private final String[] values;

    BasicToken(final int type) {
        this(type, new String[0]);
    }

    BasicToken(final int type, final String... values) {
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
