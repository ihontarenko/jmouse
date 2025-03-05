package org.jmouse.template.parser;

/**
 * Represents a context for managing parsers and options in a templating system.
 *
 * <p>The context maintains a registry of parsers that can be accessed by their class type.
 * It also allows configuration options to be set dynamically, enabling flexible parsing behavior.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ParserContext {

    ExpressionParser getExpressionParser(String name);

    void addExpressionParser(ExpressionParser parser);

    Parser getParser(String name);

    void addParser(Parser parser);

    ParserOptions getOptions();

    void setOptions(ParserOptions options);

    void clearOptions();

}
