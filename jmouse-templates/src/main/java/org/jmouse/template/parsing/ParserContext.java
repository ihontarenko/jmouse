package org.jmouse.template.parsing;

import org.jmouse.template.extension.ExtensionContainer;

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

    ExtensionContainer getExtensionContainer();

    ParserOptions getOptions();

    void setOptions(ParserOptions options);

    void clearOptions();

}
