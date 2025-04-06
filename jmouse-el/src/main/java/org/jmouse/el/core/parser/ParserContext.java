package org.jmouse.el.core.parser;

import org.jmouse.el.core.extension.ExtensionContainer;

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

    ParserOptions getOptions();

    void setOptions(ParserOptions options);

    void clearOptions();

}
