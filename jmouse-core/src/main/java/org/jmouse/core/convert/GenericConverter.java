package org.jmouse.core.convert;

import java.util.Set;

/**
 * A generic interface for converting objects of type {@code S} into objects of type
 * {@code T}. Implementations of this interface typically define a set of supported
 * source-target type pairs and provide logic for performing the actual conversion.
 *
 * <p>Usage example:
 * <pre>{@code
 * public class StringToIntegerConverter implements GenericConverter<String, Integer> {
 *
 *     @Override
 *     public Integer convert(String source, Class<String> sourceType, Class<Integer> targetType) {
 *         return source != null ? Integer.valueOf(source) : null;
 *     }
 *
 *     @Override
 *     public Set<ClassPair<String, Integer>> getSupportedTypes() {
 *         return Set.of(new ClassPair<>(String.class, Integer.class));
 *     }
 * }
 *
 * // Example usage
 * StringToIntegerConverter converter = new StringToIntegerConverter();
 * Integer result = converter.convert("42", Integer.class);  // result = 42
 * }</pre>
 *
 * @param <S> the source type
 * @param <T> the target type
 * @see ClassPair
 */
public interface GenericConverter<S, T> {

    /**
     * Converts the given source object of type {@code S} into an object of type {@code T},
     * using the provided {@code sourceType} and {@code targetType} for more specific conversion
     * logic if needed.
     *
     * @param source     the object to convert (may be {@code null})
     * @param sourceType the specific runtime type of the source object
     * @param targetType the desired target type
     * @return the converted object, or {@code null} if the source is {@code null} or cannot be converted
     */
    T convert(S source, Class<S> sourceType, Class<T> targetType);

    /**
     * A convenience method that infers the {@code sourceType} from the runtime class of
     * the given source object. If {@code source} is {@code null}, this method returns
     * {@code null}.
     *
     * @param source     the object to convert (may be {@code null})
     * @param targetType the desired target type
     * @return the converted object, or {@code null} if the source is {@code null} or cannot be converted
     */
    default T convert(S source, Class<T> targetType) {
        T converted = null;

        if (source != null) {
            @SuppressWarnings({"unchecked"})
            Class<S> sourceType = (Class<S>) source.getClass();
            converted = convert(source, sourceType, targetType);
        }

        return converted;
    }

    /**
     * Returns a set of supported source-target type pairs, indicating which conversions
     * this converter can handle. Each pair is represented by a {@link ClassPair} of
     * {@link S} and {@link T}.
     *
     * @return a set of {@code ClassPair} objects representing the supported conversions
     */
    Set<ClassPair> getSupportedTypes();

    /**
     * Creates a {@link GenericConverter} for the specified source and target types, using the provided converter logic.
     *
     * @param <S>        the source type
     * @param <T>        the target type
     * @param sourceType the source type {@link Class}
     * @param targetType the target type {@link Class}
     * @param converter  the conversion logic implemented as a {@link Converter}
     * @return a {@link GenericConverter} instance capable of converting between the specified types
     */
    static <S, T> GenericConverter<S, T> of(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? super T> converter) {
        return new GenericConverter<>() {
            @SuppressWarnings({"unchecked"})
            @Override
            public T convert(S source, Class<S> sourceType, Class<T> targetType) {
                return (T) converter.convert(source);
            }

            @Override
            public Set<ClassPair> getSupportedTypes() {
                return Set.of(new ClassPair(sourceType, targetType));
            }
        };
    }

}
