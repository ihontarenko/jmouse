package org.jmouse.core.convert;

/**
 * A functional interface for normalizing types to a standardized or expected form.
 * The {@link #normalize(Class)} method takes an input {@link Class} and returns a
 * potentially modified or generalized version of that class.
 */
@FunctionalInterface
public interface TypeNormalizer {
    /**
     * Normalizes the given class to a standardized form.
     * Implementations can use this method to map various subtypes or related types
     * to a common base type or representation.
     *
     * @param type the class to normalize
     * @return the normalized {@link Class} object; may be the same as the input type
     *         or a more general type
     */
    Class<?> normalize(Class<?> type);

    /**
     * A {@link TypeNormalizer} implementation for enum types. This normalizer
     * checks if a given class represents an enum, and if so, it returns the base
     * {@link Enum} class as the normalized type. For non-enum types, it returns
     * the input class unchanged.
     */
    class EnumTypeNormalizer implements TypeNormalizer {

        /**
         * Normalizes the given class. If the input class is an enum type, this method
         * returns the base {@link Enum} class. Otherwise, it returns the input class
         * unchanged.
         *
         * @param type the class to check and potentially normalize
         * @return {@link Enum#class} if {@code type} is an enum; otherwise, the original {@code type}
         */
        @Override
        public Class<?> normalize(Class<?> type) {
            Class<?> enumClass = type;

            if (type.isEnum()) {
                // Return the base Enum class as the normalized type for any enum
                enumClass = Enum.class;
            }

            return enumClass;
        }

    }

}
