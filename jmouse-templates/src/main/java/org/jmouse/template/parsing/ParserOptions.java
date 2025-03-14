package org.jmouse.template.parsing;

import org.jmouse.template.lexer.Token;

import java.util.function.Predicate;

/**
 * Represents configurable options for the parsing.
 *
 * <p>This interface provides mechanisms to define conditions that influence
 * the parsing process, such as specifying a stopping condition.</p>
 *
 * <pre>{@code
 * ParserOptions options = ParserOptions.withStopCondition(token -> token.is(Token.Type.SEMICOLON));
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ParserOptions {

    /**
     * Defines a stop condition that determines when the parsing should halt.
     *
     * @return a {@link Predicate} that evaluates whether parsing should stop at a given token
     */
    default Predicate<Token> stopCondition() {
        return token -> false;
    }

    /**
     * Creates a {@code ParserOptions} instance with a custom stop condition.
     *
     * @param stopCondition the stopping condition for the parsing
     * @return a new instance of {@code ParserOptions} with the specified condition
     */
    static ParserOptions withStopCondition(Predicate<Token> stopCondition) {
        return new Default(stopCondition);
    }

    /**
     * Default implementation of {@link ParserOptions}.
     */
    record Default(Predicate<Token> stopCondition) implements ParserOptions {

    }
}
