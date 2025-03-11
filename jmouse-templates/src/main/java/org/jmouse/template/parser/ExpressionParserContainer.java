package org.jmouse.template.parser;

import org.jmouse.template.AbstractExtensionContainer;

/**
 * 🏗️ A container for managing {@link ExpressionParser} instances.
 * This implementation extends {@link AbstractExtensionContainer} and uses
 * the parser's name as its unique key.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ExpressionParserContainer extends AbstractExtensionContainer<String, ExpressionParser> {

    /**
     * 🔑 Retrieves the unique key for a given {@link ExpressionParser}.
     *
     * @param extension 🛠️ the expression parser instance
     * @return 🏷️ the name of the parser, used as its key
     */
    @Override
    public String key(ExpressionParser extension) {
        return extension.getName();
    }
}
