package org.jmouse.template;

import org.jmouse.template.extension.Operator;
import org.jmouse.template.lexer.Token;

/**
 * 🏗️ A container for managing {@link Operator} instances.
 * This implementation extends {@link AbstractObjectContainer} and uses
 * the {@link Token.Type} as the unique key for each operator.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class OperatorContainer extends AbstractObjectContainer<Token.Type, Operator> {

    /**
     * 🔑 Retrieves the unique key for a given {@link Operator}.
     *
     * @param extension 🛠️ the operator instance
     * @return 🏷️ the {@link Token.Type} representing the operator's type
     */
    @Override
    public Token.Type keyFor(Operator extension) {
        return extension.getType();
    }
}
