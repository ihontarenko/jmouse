package org.jmouse.template.lexer.recognizer;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.support.EnumFinder;

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Recognizes tokens based on an enumeration type implementing {@link Token.Type}.
 *
 * <p>This recognizer searches for a matching type in the provided enum type based on a given string.</p>
 *
 * @param <T> the enum type implementing {@link Token.Type}
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class EnumTokenRecognizer<T extends Enum<T> & Token.Type> extends EnumFinder<T> implements Recognizer<Token.Type, String> {

    private final int order;
    private final T[] tokens;

    /**
     * Constructs a new enum-based type recognizer.
     *
     * @param enumType the enumeration class containing type definitions
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
     * Attempts to recognize a type from the given string.
     *
     * @param piece the string to recognize as a type
     * @return an {@link Optional} containing the recognized type, or empty if not found
     */
    @Override
    public Optional<Token.Type> recognize(String piece) {
        return ofNullable(find(piece, tokens).orElse(null));
    }
}
