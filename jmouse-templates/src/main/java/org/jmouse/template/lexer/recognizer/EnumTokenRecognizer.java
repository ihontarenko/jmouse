package org.jmouse.template.lexer.recognizer;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.support.EnumFinder;

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Recognizes tokens based on an enumeration type implementing {@link Token}.
 *
 * <p>This recognizer searches for a matching token in the provided enum type based on a given string.</p>
 *
 * @param <T> the enum type implementing {@link Token}
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class EnumTokenRecognizer<T extends Enum<T> & Token> extends EnumFinder<T> implements Recognizer<Token, String> {

    private final int order;
    private final T[] tokens;

    /**
     * Constructs a new enum-based token recognizer.
     *
     * @param enumType the enumeration class containing token definitions
     * @param order    the recognition order priority
     */
    public EnumTokenRecognizer(Class<T> enumType, int order) {
        this.order = order;
        this.tokens = enumType.getEnumConstants();
    }

    /**
     * Returns the recognition order priority.
     *
     * @return the recognition order value
     */
    @Override
    public int getOrder() {
        return order;
    }

    /**
     * Attempts to recognize a token from the given string.
     *
     * @param piece the string to recognize as a token
     * @return an {@link Optional} containing the recognized token, or empty if not found
     */
    @Override
    public Optional<Token> recognize(String piece) {
        return ofNullable(find(piece, tokens).orElse(null));
    }
}
