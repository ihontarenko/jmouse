package org.jmouse.template.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a context for managing parsers and options in a templating system.
 *
 * <p>The context maintains a registry of parsers that can be accessed by their class type.
 * It also allows configuration options to be set dynamically, enabling flexible parsing behavior.</p>
 *
 * <pre>{@code
 * ParserContext context = ParserContext.newContext();
 * context.addParser(new ArithmeticParser());
 * Parser parser = context.getParser(ArithmeticParser.class);
 * context.setOptions(new ParserOptions());
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ParserContext {

    /**
     * Creates a new instance of {@link ParserContext}.
     *
     * @return a new parser context instance
     */
    static ParserContext newContext() {
        return new SimpleContext();
    }

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
     * Retrieves the current parser options.
     *
     * @return the current {@link ParserOptions} instance or {@code null} if not set
     */
    ParserOptions getOptions();

    /**
     * Sets the parser options for this context.
     *
     * @param options the {@link ParserOptions} instance to apply
     */
    void setOptions(ParserOptions options);

    /**
     * Clears the parser options from this context.
     */
    void clearOptions();

    /**
     * A simple implementation of {@link ParserContext} that maintains a registry of parsers and options.
     */
    class SimpleContext implements ParserContext {

        private final Map<Class<? extends Parser>, Parser> parsers = new HashMap<>();
        private       ParserOptions                        options;

        /**
         * Retrieves the currently set parser options.
         *
         * @return the current parser options, or {@code null} if not set
         */
        @Override
        public ParserOptions getOptions() {
            return options;
        }

        /**
         * Sets the parser options for this context.
         *
         * @param options the parser options to apply
         */
        @Override
        public void setOptions(ParserOptions options) {
            this.options = options;
        }

        /**
         * Clears the currently stored parser options.
         */
        @Override
        public void clearOptions() {
            this.options = null;
        }

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
