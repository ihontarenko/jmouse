package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * Represents the "|" filter operator ({@code IS}).
 * <p>
 * Example:
 * <pre>{@code
 *     x | int
 *     name | upper
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
public enum FilterOperator implements Operator {

    /**
     * Test operator ({@code T_VERTICAL_SLASH}), used for applying tests to values.
     */
    FILTER(BasicToken.T_VERTICAL_SLASH, "FILTER", 850);

    private final Token.Type type;
    private final String     name;
    private final int        precedence;

    FilterOperator(Token.Type type, String name, int precedence) {
        this.type = type;
        this.name = name;
        this.precedence = precedence;
    }

    @Override
    public int getPrecedence() {
        return precedence;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Token.Type getType() {
        return type;
    }

}
