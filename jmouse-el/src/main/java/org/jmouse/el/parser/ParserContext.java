package org.jmouse.el.parser;

import org.jmouse.core.context.ContextScope;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

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
     * Whether the token after a complete operand ends the expression instead of continuing it: a word an imported
     * extension declared a boundary ({@link org.jmouse.el.extension.Extension#getExpressionBoundaries()}). A text in
     * quotes is never one.
     *
     * @param token the token after a complete operand
     * @return {@code true} when the expression ends before it
     */
    default boolean endsExpression(Token token) {
        return token.type() != BasicToken.T_STRING && isExpressionBoundary(token.value());
    }

    ParserOptions getOptions();

    ContextScope<Class<? extends Parser>> getContextScope();

    void setOptions(ParserOptions options);

    void clearOptions();

}
