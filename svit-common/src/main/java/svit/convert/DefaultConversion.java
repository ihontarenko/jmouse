package svit.convert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.graph.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default implementation of the {@link Conversion} interface. It manages a registry
 * of {@link GenericConverter} instances mapped to specific {@link ClassPair}s, allowing
 * for dynamic registration and retrieval of converters. This implementation uses a
 * concurrent hash map to safely handle converters in multi-threaded environments.
 *
 * @see Conversion
 * @see GenericConverter
 * @see ConverterNotFound
 */
public class DefaultConversion implements Conversion {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConversion.class);

    private final Map<ClassPair<?, ?>, GenericConverter<?, ?>> converters = new ConcurrentHashMap<>();
    private final TypeNormalizer                               normalizer = new TypeNormalizer.EnumTypeNormalizer();
    private final Graph<Class<?>>                              graph      = new MapListGraph<>();
    private final PathFinder<Class<?>>                         pathFinder = new BFSPathFinder<>();

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
            graph.addEdge(supportedType.getClassA(), supportedType.getClassB());
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
    @SuppressWarnings({"unchecked"})
    public <T, R> R convert(T source, Class<T> sourceType, Class<R> targetType) {
        R converted = null;

        if (source != null) {
            @SuppressWarnings({"unchecked"})
            Class<R>        normalizedType = (Class<R>) normalizer.normalize(targetType);
            ClassPair<T, R> classPair      = new ClassPair<>(sourceType, normalizedType);

            // find direct converter
            GenericConverter<T, R> converter = getConverter(classPair);

            if (converter == null) {
                // search transition converters chain
                // using graph of type converter type and BFS search
                List<ClassPair<?, ?>>      transitions = searchTransitionChain(sourceType, targetType);
                List<Converter<Object, R>> chain       = new ArrayList<>();

                if (transitions.isEmpty()) {
                    throw new ConverterNotFound(classPair);
                }

                for (ClassPair<?, ?> transition : transitions) {
                    ClassPair<Object, Object> pair = (ClassPair<Object, Object>) transition;
                    chain.add(value -> (R) getConverter(pair).convert(value, pair.getClassB()));
                }

                Optional<Converter<Object, R>> composite = chain.stream().reduce(Converter::andThen);

                converted = composite.get().convert(source);
            } else {
                converted = converter.convert(source, targetType);
            }

        }

        return converted;
    }

    /**
     * Searches for a chain of {@link GenericConverter}s that can transition from the source type
     * {@code sourceType} to the target type {@code targetType}. This method uses a path-finding
     * algorithm on a graph of types to determine a sequence of intermediate types connecting
     * the source and target. For each consecutive pair of types in the found path, it retrieves
     * a corresponding {@link GenericConverter} and adds it to the conversion chain.
     *
     * @param <S> the initial source type
     * @param <T> the final target type
     * @param sourceType the class representing the starting type of the conversion
     * @param targetType the class representing the desired final type of the conversion
     * @return a list of {@link GenericConverter} instances representing the conversion path
     *         from {@code sourceType} to {@code targetType}, or an empty list if no such path exists
     */
    @Override
    public <S, T> List<ClassPair<?, ?>> searchTransitionChain(Class<S> sourceType, Class<T> targetType) {
        List<Class<?>>        typePath = pathFinder.findPath(graph, sourceType, targetType);
        List<ClassPair<?, ?>> chain    = new ArrayList<>();

        if (!typePath.isEmpty()) {
            LOGGER.info("Transition path for {} was found.", new ClassPair<>(sourceType, targetType));

            for (Edge<Class<?>> edge : Graph.toEdges(typePath)) {
                ClassPair<?, ?> classPair = new ClassPair<>(edge.nodeA(), edge.nodeB());
                LOGGER.info("Transition: {}", classPair);
                chain.add(classPair);
            }
        }

        return chain;
    }
}
