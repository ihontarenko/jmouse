package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

public enum InOperator implements Operator {

    IN(BasicToken.T_IN, "IN", 500);

    private final Token.Type type;
    private final String     name;
    private final int        precedence;

    InOperator(Token.Type type, String name, int precedence) {
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
