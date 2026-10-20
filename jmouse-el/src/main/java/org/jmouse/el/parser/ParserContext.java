package org.jmouse.el.parser;

import org.jmouse.core.context.ContextScope;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

/**
 * Represents a evaluation for managing parsers and options in a templating system.
 *
 * <p>The evaluation maintains a registry of parsers that can be accessed by their class type.
 * It also allows configuration options to be set dynamically, enabling flexible parser behavior.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ParserContext extends ExtensionContainer {

    ExtensionContainer getExtensionContainer();

    /**
     * Whether the token the cursor stands on, after a complete operand, ends the expression instead of continuing it: a
     * word an imported extension declared a boundary ({@link org.jmouse.el.extension.Extension#getExpressionBoundaries()}).
     * A text in quotes is never one, and neither is a word called — {@code 2 log(x)} multiplies by a call.
     *
     * @param cursor the cursor, on the token after a complete operand
     * @return {@code true} when the expression ends before it
     */
    default boolean endsExpression(TokenCursor cursor) {
        Token token = cursor.peek();
        return token.type() != BasicToken.T_STRING && isExpressionBoundary(token.value())
                && !cursor.checkAt(1, BasicToken.T_OPEN_PAREN);
    }

    ParserOptions getOptions();

    ContextScope<Class<? extends Parser>> getContextScope();

    void setOptions(ParserOptions options);

    void clearOptions();

}
