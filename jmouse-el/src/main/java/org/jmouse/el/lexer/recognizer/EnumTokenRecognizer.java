package org.jmouse.el.lexer.recognizer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.support.EnumFinder;

import java.util.HashMap;
import java.util.Map;
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

    private final int            order;
    /** Each template's type, by the template as {@link #folded} folds it: the type declared first where two share one. */
    private final Map<String, T> byTemplate = new HashMap<>();

    /**
     * Constructs a new enum-based type recognizer.
     *
     * @param enumType the enumeration class containing type definitions
     * @param order    the recognition order priority
     */
    public EnumTokenRecognizer(Class<T> enumType, int order) {
        this.order = order;
        for (T token : enumType.getEnumConstants()) {
            for (String template : token.getTokenTemplates()) {
                byTemplate.putIfAbsent(folded(template), token);
            }
        }
    }

    /**
     * A word with its case folded as {@link String#equalsIgnoreCase} compares it — each character upper-cased, then lower-cased —:
     * two words are equal ignoring case exactly when their folded forms are equal.
     */
    private static String folded(String word) {
        StringBuilder folded = new StringBuilder(word.length());
        for (int index = 0; index < word.length(); index++) {
            folded.append(Character.toLowerCase(Character.toUpperCase(word.charAt(index))));
        }
        return folded.toString();
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
        return ofNullable(byTemplate.get(folded(rawToken.value().trim())));
    }
}
