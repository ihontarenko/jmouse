package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

public enum AttributeAccessOperator implements Operator {

    ACCESS(BasicToken.T_BACKSLASH, "attribute_access", 200);

    private final Token.Type         type;
    private final String             name;
    private final int                precedence;

    AttributeAccessOperator(Token.Type type, String name, int precedence) {
        this.type = type;
        this.name = name;
        this.precedence = precedence;
    }

    /**
     * ⚖️ Returns the precedence level of this operator.
     * Higher values indicate higher precedence in tag evaluation.
     *
     * @return 🔢 the precedence level of the operator
     */
    @Override
    public int getPrecedence() {
        return precedence;
    }

    /**
     * 🏷️ Returns the name of the operator.
     *
     * @return 📛 the name of the operator
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 🔠 Returns the {@link Token.Type} associated with this operator.
     *
     * @return 🎭 the token type representing this operator
     */
    @Override
    public Token.Type getType() {
        return type;
    }

}
