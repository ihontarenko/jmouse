package org.jmouse.template.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a context for managing parsers in a templating system.
 *
 * <p>The context maintains a registry of parsers that can be accessed by their class type.
 * This allows different parser implementations to be managed dynamically.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ParserContext {

    /**
     * Retrieves a registered parser by its class type.
     *
     * @param type the class of the requested parser
     * @return the corresponding parser instance
     * @throws ParserException if no parser is found for the given type
     */
    Parser getParser(Class<? extends Parser> type);

    /**
     * Registers a parser in the context.
     *
     * @param parser the parser instance to register
     */
    void addParser(Parser parser);

    /**
     * Creates a new instance of {@link ParserContext}.
     *
     * @return a new parser context instance
     */
    static ParserContext newContext() {
        return new SimpleContext();
    }

    /**
     * A simple implementation of {@link ParserContext} that maintains a registry of parsers.
     */
    class SimpleContext implements ParserContext {

        private final Map<Class<? extends Parser>, Parser> parsers = new HashMap<>();

        /**
         * Registers a parser in the context.
         *
         * @param parser the parser instance to register
         */
        @Override
        public void addParser(Parser parser) {
            parsers.put(parser.getClass(), parser);
        }

        /**
         * Retrieves a parser from the context by its class type.
         *
         * @param type the class of the requested parser
         * @return the corresponding parser instance
         * @throws ParserException if no parser is found for the given type
         */
        @Override
        public Parser getParser(Class<? extends Parser> type) {
            if (!parsers.containsKey(type)) {
                throw new ParserException("No parser found for %s".formatted(type));
            }
            return parsers.get(type);
        }
    }
}
