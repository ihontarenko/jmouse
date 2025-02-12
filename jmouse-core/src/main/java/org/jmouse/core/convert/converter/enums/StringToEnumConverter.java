package org.jmouse.core.convert.converter.enums;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

/**
 * A {@link GenericConverter} implementation that converts a {@link String} value
 * into a corresponding {@link Enum} constant by matching names (case-insensitive).
 * If no matching constant is found, this converter returns {@code null}.
 *
 * <p>Usage example:
 * <pre>{@code
 * // Suppose you have an enum:
 * enum Color { RED, GREEN, BLUE }
 *
 * // Create an instance of the converter
 * StringToEnumConverter converter = new StringToEnumConverter();
 *
 * // Convert a string to the enum constant
 * Color color = (Color) converter.convert("green", Color.class);
 * // 'color' will be Color.GREEN
 * }</pre>
 *
 * @see GenericConverter
 */
public class StringToEnumConverter implements GenericConverter<String, Enum<?>> {

    /**
     * Converts the given {@code source} string into an enum constant by iterating
     * through the enum's constants and matching by name (case-insensitive). If no
     * match is found, {@code null} is returned.
     *
     * @param source     the {@code String} value to convert
     * @param sourceType the specific runtime type of the source bean (String)
     * @param targetType the enum class type to which the string should be converted
     * @return the matching enum constant, or {@code null} if none matches
     */
    @Override
    public Enum<?> convert(String source, Class<String> sourceType, Class<Enum<?>> targetType) {
        Enum<?> constant = null;

        for (Enum<?> enumConstant : targetType.getEnumConstants()) {
            if (enumConstant.name().equalsIgnoreCase(source)) {
                constant = enumConstant;
                break;
            }
        }

        return constant;
    }

    /**
     * Returns a set containing one {@link ClassPair} representing support for
     * converting from {@link String} to {@code Enum<?>}. This indicates that
     * any {@code Enum} class is a potential target type, as long as the source
     * is a {@link String}.
     *
     * @return a set containing a single {@code ClassPair} for {@code String -> Enum<?>}
     */
    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(new ClassPair(String.class, Enum.class));
    }

}
