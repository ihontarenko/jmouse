package org.jmouse.core.convert;

import java.util.List;

/**
 * A factory interface for registering and retrieving {@link Converter} or
 * {@link GenericConverter} instances. Implementations of this interface can
 * maintain a registry of source-target type pairs, providing flexible and
 * extensible type conversion capabilities.
 *
 * <p><strong>Usage example:</strong></p>
 * <pre>{@code
 * // 1. Create a ConverterFactory implementation
 * ConverterFactory factory = new DefaultConverterFactory();
 *
 * // 2. Register a simple Converter for Integer -> String
 * factory.registerConverter(Integer.class, String.class, Object::toString);
 *
 * // 3. Register a GenericConverter
 * factory.registerConverter(new StringToEnumConverter());
 *
 * // 4. Retrieve and use the converters
 *
 * // Using getConverter(Class<S>, Class<T>)
 * GenericConverter<Integer, String> intToStr =
 *         factory.getConverter(Integer.class, String.class);
 * String result = intToStr.convert(42, String.class); // result = "42"
 *
 * // Or using getConverter(ClassPair<S, T>)
 * ClassPair<Integer, String> classPair = new ClassPair<>(Integer.class, String.class);
 * GenericConverter<Integer, String> pairConverter = factory.getConverter(classPair);
 * String pairResult = pairConverter.convert(42, String.class); // also "42"
 * }</pre>
 *
 * @see Converter
 * @see GenericConverter
 * @see ClassPair
 */
public interface ConverterFactory {

    /**
     * Registers a simple {@link Converter} for the given source and target types.
     *
     * @param <S>       the source type
     * @param <T>       the target type
     * @param sourceType the class representing the source type
     * @param targetType the class representing the target type
     * @param converter  the converter instance
     */
    <S, T> void registerConverter(Class<S> sourceType, Class<T> targetType, Converter<S, T> converter);

    /**
     * Registers a {@link GenericConverter} that can potentially handle multiple
     * source-target type pairs, as indicated by its {@link GenericConverter#getSupportedTypes()}
     * method.
     *
     * @param genericConverter the generic converter to register
     */
    void registerConverter(GenericConverter<?, ?> genericConverter);

    /**
     * Checks if a converter is registered for the given source and target types.
     *
     * @param sourceType the class representing the source type
     * @param targetType the class representing the target type
     * @return {@code true} if a converter exists, {@code false} otherwise
     */
    default boolean hasConverter(Class<?> sourceType, Class<?> targetType) {
        return hasConverter(new ClassPair(sourceType, targetType));
    }

    /**
     * Checks if a converter exists for the specified {@link ClassPair}.
     *
     * @param classPair a {@link ClassPair} representing the source and target types
     * @return {@code true} if a converter exists, {@code false} otherwise
     */
    default boolean hasConverter(ClassPair classPair) {
        return getConverter(classPair) != null;
    }

    /**
     * Removes a registered converter for the specified source and target types.
     *
     * @param classPair a {@link ClassPair} representing the source and target types
     * @return {@code true} if a converter was removed, {@code false} if no such converter was found
     */
    boolean removeConverter(ClassPair classPair);

    /**
     * Retrieves a {@link GenericConverter} instance capable of handling the specified
     * source and target types, if available. This default method constructs a
     * {@link ClassPair} internally and delegates to {@link #getConverter(ClassPair)}.
     *
     * @param <S>       the source type
     * @param <T>       the target type
     * @param sourceType the class representing the source type
     * @param targetType the class representing the target type
     * @return a compatible generic converter, or {@code null} if none is found
     */
    default <S, T> GenericConverter<S, T> getConverter(Class<S> sourceType, Class<T> targetType) {
        return getConverter(new ClassPair(sourceType, targetType));
    }

    /**
     * Retrieves a {@link GenericConverter} instance using a {@link ClassPair} directly,
     * allowing you to specify both the source and target types together. This method is
     * typically called by the default {@link #getConverter(Class, Class)} method.
     *
     * @param <S>       the source type
     * @param <T>       the target type
     * @param classPair a {@link ClassPair} representing the source and target types
     * @return a compatible generic converter, or {@code null} if none is found
     */
    <S, T> GenericConverter<S, T> getConverter(ClassPair classPair);

    /**
     * Searches for a transition chain that allows conversion from {@code sourceType} to {@code targetType}
     * using intermediate conversion steps. If a direct conversion is not available, this method
     * attempts to construct a path of conversions that ultimately leads to the desired type.
     *
     * <p>This is useful when multiple converters exist that can form a chain of type transformations.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Suppose we have registered converters for:
     * // String -> Integer
     * // Integer -> Double
     * // Double -> BigDecimal
     *
     * List<ClassPair<?, ?>> transitions = factory.searchTransitionChain(String.class, BigDecimal.class);
     * // Resulting chain: [String -> Integer, Integer -> Double, Double -> BigDecimal]
     * }</pre>
     *
     * @param <S>       the source type
     * @param <T>       the target type
     * @param sourceType the class representing the source type
     * @param targetType the class representing the target type
     * @return a list of {@link ClassPair} instances representing the conversion path,
     *         or an empty list if no transition is found
     */
    <S, T> List<ClassPair> searchTransitionChain(Class<S> sourceType, Class<T> targetType);

}
