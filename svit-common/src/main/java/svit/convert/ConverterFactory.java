package svit.convert;

/**
 * A factory interface for registering and retrieving {@link Converter} or
 * {@link GenericConverter} instances. Implementations of this interface can
 * maintain a registry of source-target type pairs, providing flexible and
 * extensible type conversion capabilities.
 *
 * <p>Usage example:
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
        return getConverter(new ClassPair<>(sourceType, targetType));
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
    <S, T> GenericConverter<S, T> getConverter(ClassPair<S, T> classPair);

}
