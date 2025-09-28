package org.jmouse.el.lexer.recognizer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.support.EnumFinder;

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
public class EnumTokenRecognizer<T extends Enum<T> & Token.Type> extends EnumFinder<T> implements Recognizer<Token.Type, RawToken> {

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
    public int order() {
        return order;
    }

    /**
     * Attempts to recognize a type from the given string.
     */
    @Override
    public Optional<Token.Type> recognize(RawToken rawToken) {
        return ofNullable(find(rawToken.value().trim(), tokens).orElse(null));
    }
}
