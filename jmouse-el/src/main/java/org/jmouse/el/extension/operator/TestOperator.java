package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

/**
 * Represents the "IS" test operator ({@code IS}).
 * <p>
 * Example:
 * <pre>{@code
 *     x IS even
 *     name IS not empty
 * }</pre>
 *
 * @author Ivan Hontarenko
 */
public enum TestOperator implements Operator {

    /**
     * Test operator ({@code IS}), used for applying tests to values.
     */
    IS(BasicToken.T_IS, "IS", 9);

    private final Token.Type type;
    private final String     name;
    private final int        precedence;

    TestOperator(Token.Type type, String name, int precedence) {
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
