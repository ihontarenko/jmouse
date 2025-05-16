package org.jmouse.el.extension.operator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.calculator.ConcatCalculator;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

public enum ConcatOperator implements Operator {

    CONCAT(new ConcatCalculator(), BasicToken.T_TILDA, "concat", 550);

    private final Calculator<String> calculator;
    private final Token.Type         type;
    private final String             name;
    private final int                precedence;

    ConcatOperator(Calculator<String> calculator, Token.Type type, String name, int precedence) {
        this.calculator = calculator;
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

    /**
     * Returns the associated calculator for this operator.
     *
     * @return The calculator instance.
     */
    @Override
    public Calculator<?> getCalculator() {
        return calculator;
    }

}
