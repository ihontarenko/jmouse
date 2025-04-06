package org.jmouse.el.core.lexer.support;

import org.jmouse.el.core.lexer.Token;

import java.util.Optional;

/**
 * 🔍 Utility class for searching an enumeration constant based on string value.
 * <p>
 * This generic finder is designed to work with enums that implement {@link Token.Type}.
 *
 * @param <T> The enum type that extends {@link Token.Type}
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class EnumFinder<T extends Enum<T> & Token.Type> implements Finder<T, String, T[]> {

    /**
     * Finds an enum constant from the provided source based on a string value.
     *
     * @param value  🔠 The string value to search for
     * @param source 🎭 The array of enum constants
     * @return 🧐 An {@link Optional} containing the matched enum constant, or {@code Optional.empty()} if not found
     */
    @Override
    public Optional<T> find(String value, T[] source) {
        if (source == null || value == null) {
            return Optional.empty();
        }

        for (T current : source) {
            for (String template : current.getTokenTemplates()) {
                if (template.equalsIgnoreCase(value)) {
                    // Return immediately when found
                    return Optional.of(current);
                }
            }
        }

        return Optional.empty();
    }
}
