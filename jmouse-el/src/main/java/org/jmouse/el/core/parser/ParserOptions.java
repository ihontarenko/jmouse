package org.jmouse.el.core.parser;

import org.jmouse.el.core.lexer.TokenCursor;

import java.util.function.Predicate;

/**
 * Represents configurable options for the parser.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ParserOptions {

    /**
     * Defines a stop condition that determines when the parser should halt.
     *
     * @return a {@link Predicate} that evaluates whether parser should stop at a given token
     */
    default Predicate<TokenCursor> stopCondition() {
        return token -> false;
    }

    default Class<? extends Parser> nextParser() {
        return null;
    }

    /**
     * Creates a {@code ParserOptions} instance with a custom stop condition.
     *
     * @param stopCondition the stopping condition for the parser
     * @return a new instance of {@code ParserOptions} with the specified condition
     */
    static ParserOptions withStopCondition(Predicate<TokenCursor> stopCondition) {
        return new Default(stopCondition, null);
    }

    static ParserOptions withNextParser(Class<? extends Parser> nextParser) {
        return new Default(null, nextParser);
    }

    /**
     * DirectAccess implementation of {@link ParserOptions}.
     */
    record Default(Predicate<TokenCursor> stopCondition, Class<? extends Parser> nextParser) implements ParserOptions {

    }
}
