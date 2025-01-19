package svit.convert;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default implementation of the {@link Conversion} interface. It manages a registry
 * of {@link GenericConverter} instances mapped to specific {@link ClassPair}s, allowing
 * for dynamic registration and retrieval of converters. This implementation uses a
 * concurrent hash map to safely handle converters in multi-threaded environments.
 *
 * <p>The {@code DefaultConversion} supports:
 * <ul>
 *   <li>Registering simple converters using source and target types with {@link #registerConverter(Class, Class, Converter)}</li>
 *   <li>Registering generic converters with {@link #registerConverter(GenericConverter)}</li>
 *   <li>Retrieving converters based on type pairs with {@link #getConverter(ClassPair)}</li>
 *   <li>Converting objects from one type to another with {@link #convert(Object, Class, Class)}</li>
 * </ul>
 *
 * @see Conversion
 * @see GenericConverter
 * @see ConverterNotFound
 */
public class DefaultConversion implements Conversion {

    private final Map<ClassPair<?, ?>, GenericConverter<?, ?>> converters = new ConcurrentHashMap<>();
    private final TypeNormalizer                               normalizer = new TypeNormalizer.EnumTypeNormalizer();

    /**
     * Registers a simple {@link Converter} for converting from {@code sourceType} to {@code targetType}.
     * Internally wraps the provided {@code Converter} into a {@link GenericConverter} to support the
     * registration mechanism.
     *
     * @param <S>        the source type
     * @param <T>        the target type
     * @param sourceType the source class
     * @param targetType the target class
     * @param converter  the converter instance to register
     */
    @Override
    public <S, T> void registerConverter(Class<S> sourceType, Class<T> targetType, Converter<S, T> converter) {
        registerConverter(new GenericConverter<S, T>() {
            @Override
            public T convert(S source, Class<S> sourceType, Class<T> targetType) {
                return converter.convert(source);
            }

            @Override
            public Set<ClassPair<? extends S, ? extends T>> getSupportedTypes() {
                return Set.of(new ClassPair<>(sourceType, targetType));
            }
        });
    }

    /**
     * Registers a {@link GenericConverter} by adding it to the internal registry. For each supported
     * type pair returned by the converter, the converter is associated with that type pair in the registry.
     *
     * @param genericConverter the generic converter to register
     */
    @Override
    public void registerConverter(GenericConverter<?, ?> genericConverter) {
        for (ClassPair<?, ?> supportedType : genericConverter.getSupportedTypes()) {
            converters.putIfAbsent(supportedType, genericConverter);
        }
    }

    /**
     * Retrieves a {@link GenericConverter} capable of converting from the source type to the target type,
     * as specified by the given {@link ClassPair}. If no converter is found, {@code null} is returned.
     *
     * @param <S>       the source type
     * @param <T>       the target type
     * @param classPair the pair of source and target classes
     * @return a suitable {@link GenericConverter}, or {@code null} if none is registered
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public <S, T> GenericConverter<S, T> getConverter(ClassPair<S, T> classPair) {
        return (GenericConverter<S, T>) converters.get(classPair);
    }

    /**
     * Converts the given {@code source} object from type {@code T} to type {@code R} using a registered converter.
     * It first looks up a converter based on the source and target types. If a converter is found,
     * it uses the converter to perform the conversion. If no converter is found, a {@link ConverterNotFound}
     * exception is thrown.
     *
     * @param <T>        the type of the source object
     * @param <R>        the type of the desired result
     * @param source     the object to convert
     * @param sourceType the class representing the source type
     * @param targetType the class representing the target type
     * @return the converted object of type {@code R}, or {@code null} if the source is {@code null}
     * @throws ConverterNotFound if no converter is registered for the given source and target types
     */
    @Override
    public <T, R> R convert(T source, Class<T> sourceType, Class<R> targetType) {
        R converted = null;

        if (source != null) {
            @SuppressWarnings({"unchecked"})
            Class<R>        normalizedType = (Class<R>) normalizer.normalize(targetType);
            ClassPair<T, R> classPair      = new ClassPair<>(sourceType, normalizedType);

            GenericConverter<T, R> converter = getConverter(classPair);

            if (converter == null) {
                throw new ConverterNotFound(classPair);
            }

            converted = converter.convert(source, targetType);
        }

        return converted;
    }
}
