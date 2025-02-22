package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

/**
 * A {@link GenericConverter} implementation that converts various numeric types
 * ({@link Number}) into their corresponding {@link String} representation. This
 * converter is especially useful for formatting numeric values into human-readable
 * text, logging, or serialization.
 *
 * <p>The supported numeric types include:
 * <ul>
 *   <li>{@link Short}</li>
 *   <li>{@link Integer}</li>
 *   <li>{@link Long}</li>
 *   <li>{@link Double}</li>
 *   <li>{@link Float}</li>
 *   <li>{@link BigInteger}</li>
 *   <li>{@link BigDecimal}</li>
 * </ul>
 *
 * @see GenericConverter
 */
public class NumberToStringConverter implements GenericConverter<Number, String> {

    /**
     * Converts the given numeric value to a {@code String} by calling {@code String.valueOf(source)}.
     *
     * @param source     the numeric value to convert
     * @param sourceType the specific runtime type of the source structured
     * @param targetType the desired target type, which is always {@link String} in this case
     * @return the string representation of {@code source}
     */
    @Override
    public String convert(Number source, Class<Number> sourceType, Class<String> targetType) {
        return String.valueOf(source);
    }

    /**
     * Returns a set of {@link ClassPair}s representing all the numeric-to-String conversions
     * this converter supports.
     *
     * @return a set of supported source-target type pairs
     */
    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                new ClassPair(Short.class, String.class),
                new ClassPair(Integer.class, String.class),
                new ClassPair(Long.class, String.class),
                new ClassPair(Double.class, String.class),
                new ClassPair(Float.class, String.class),
                new ClassPair(BigInteger.class, String.class),
                new ClassPair(BigDecimal.class, String.class)
        );
    }

}
