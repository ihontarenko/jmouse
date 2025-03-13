package org.jmouse.template.parser;

import org.jmouse.template.extension.ExtensionContainer;
import org.jmouse.template.extension.Operator;
import org.jmouse.template.lexer.Token;

/**
 * Represents a context for managing parsers and options in a templating system.
 *
 * <p>The context maintains a registry of parsers that can be accessed by their class type.
 * It also allows configuration options to be set dynamically, enabling flexible parsing behavior.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ParserContext extends ExtensionContainer {

    ParserOptions getOptions();

    void setOptions(ParserOptions options);

    void clearOptions();

}
