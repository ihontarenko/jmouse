package org.jmouse.el.core.extension;

import org.jmouse.el.core.lexer.Token;

/**
 * 🔢 Represents an operator used in expressions.
 * Operators have precedence levels, names, and are associated with a specific token type.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Operator {

    /**
     * ⚖️ Returns the precedence level of this operator.
     * Higher values indicate higher precedence in tag evaluation.
     *
     * @return 🔢 the precedence level of the operator
     */
    int getPrecedence();

    /**
     * 🏷️ Returns the name of the operator.
     *
     * @return 📛 the name of the operator
     */
    String getName();

    /**
     * 🔠 Returns the {@link Token.Type} associated with this operator.
     *
     * @return 🎭 the token type representing this operator
     */
    Token.Type getType();

    /**
     * Returns the associated calculator for this operator.
     *
     * @return The calculator instance.
     */
    default Calculator<?> getCalculator() {
        throw new UnsupportedOperationException("No corresponding calculator found for: '%s' operator".formatted(getName()));
    }
}
