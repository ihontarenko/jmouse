package org.jmouse.core.convert.converter.enums;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.GenericConverter;

import java.util.Set;

/**
 * A converter that maps {@link Integer} values to corresponding {@link Enum} constants.
 * <p>
 * This converter uses the ordinal value of the enum constants to perform the conversion. The integer value must
 * match the ordinal of one of the constants in the target enum type.
 * </p>
 *
 * @see GenericConverter
 */
public class EnumToScalarConverter implements GenericConverter<Enum<?>, Object> {

    /**
     * Converts an {@link Integer} to the corresponding {@link Enum} constant based on its ordinal value.
     *
     * @param source     the source integer to convert
     * @param sourceType the source type, expected to be {@link Integer}
     * @param targetType the target type, expected to be a subclass of {@link Enum}
     * @return the matching {@link Enum} constant, or {@code null} if no match is found
     * <p>
     * This method iterates through the constants of the target enum type and returns the constant
     * with an ordinal value equal to the source integer.
     * </p>
     * @throws IllegalArgumentException if the target type is not an enum
     * @see Enum#ordinal()
     */
    @Override
    public Object convert(Enum<?> source, Class<Enum<?>> sourceType, Class<Object> targetType) {
        Object value = null;

        for (Enum<?> enumConstant : source.getClass().getEnumConstants()) {
            if (enumConstant == source) {

                if (String.class.isAssignableFrom(targetType)) {
                    value = enumConstant.name();
                } else if (Integer.class.isAssignableFrom(targetType)) {
                    value = enumConstant.ordinal();
                }

                break;
            }
        }

        return value;
    }

    /**
     * Returns the set of supported type pairs for this converter.
     *
     * @return a set containing the {@link ClassPair} of {@link Integer} and {@link Enum}
     * <p>
     * This method defines the mapping of {@link Integer} to {@link Enum} as a supported conversion pair.
     * </p>
     * @see ClassPair
     */
    @Override
    public Set<ClassPair> getSupportedTypes() {
        return Set.of(
                new ClassPair(Enum.class, String.class),
                new ClassPair(Enum.class, Integer.class)
        );
    }

}
