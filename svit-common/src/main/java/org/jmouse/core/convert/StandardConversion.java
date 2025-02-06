package org.jmouse.core.convert;

import org.jmouse.core.graph.*;
import org.jmouse.core.reflection.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.joining;

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
public class StandardConversion implements Conversion {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardConversion.class);

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
        registerConverter(GenericConverter.of(sourceType, targetType, converter));
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
            graph.addEdge(supportedType.classA(), supportedType.classB());
            converters.putIfAbsent(supportedType, genericConverter);
        }
    }

    /**
     * Checks if a converter exists for the specified {@link ClassPair}.
     *
     * @param classPair a {@link ClassPair} representing the source and target types
     * @return {@code true} if a converter exists, {@code false} otherwise
     */
    @Override
    public boolean hasConverter(ClassPair<?, ?> classPair) {
        return converters.containsKey(classPair);
    }

    /**
     * Removes a registered converter for the specified source and target types.
     *
     * @param classPair a {@link ClassPair} representing the source and target types
     * @return {@code true} if a converter was removed, {@code false} if no such converter was found
     */
    @Override
    public boolean removeConverter(ClassPair<?, ?> classPair) {
        boolean removed = false;

        if (hasConverter(classPair)) {
            removed = converters.remove(classPair) != null;
        }

        return removed;
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
     * <p>
     * The method first looks up a converter based on the source and target types. If a converter is found,
     * it performs the conversion. If no direct converter is available, the method attempts to find a suitable converter
     * through inherited types or by searching for a transition chain using a graph of type converters. If no converter
     * is found, a {@link ConverterNotFound} exception is thrown.
     * </p>
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

            if (classPair.isTheSame()) {
                return (R) source;
            }

            // Try to find a direct converter
            GenericConverter<T, R> converter = getConverter(classPair);

            if (converter == null) {
                // Attempt to find a converter using inherited types
                ClassPair<T, R> candidate = searchPossibleCandidate(sourceType, targetType);

                if (candidate != null) {
                    classPair = candidate;
                    converter = getConverter(classPair);
                }
            }

            if (converter == null) {
                // Search for a transition chain using a graph of converters and BFS search
                List<ClassPair<?, ?>> transitions = searchTransitionChain(sourceType, targetType);

                if (transitions.isEmpty()) {
                    throw new ConverterNotFound(classPair);
                }

                LOGGER.info("Conversion transitions: {}", transitions);

                Object intermediate = source;

                for (ClassPair<?, ?> transition : transitions) {
                    ClassPair<Object, Object> pair = (ClassPair<Object, Object>) transition;
                    intermediate = getConverter(pair).convert(intermediate, pair.classB());
                }

                converted = (R) intermediate;
            } else {
                LOGGER.info("Converter: {}", classPair);
                converted = converter.convert(source, targetType);
            }

        }

        return converted;
    }

    /**
     * Searches for a potential converter candidate by checking interfaces and superclasses of the source and target types.
     * <p>
     * The method iterates over the interfaces and classes of the source and target types to find a {@link ClassPair}
     * that matches a registered converter.
     * </p>
     *
     * @param <S>         the source type
     * @param <T>         the target type
     * @param sourceType  the class representing the source type
     * @param targetType  the class representing the target type
     * @return a {@link ClassPair} containing the source and target types if a suitable converter is found,
     *         {@code null} if no converter is found
     */
    public <S, T> ClassPair<S, T> searchPossibleCandidate(Class<S> sourceType, Class<T> targetType) {
        List<Class<?>> sourceCandidates = new ArrayList<>(List.of(Reflections.getClassInterfaces(sourceType)));
        List<Class<?>> targetCandidates = new ArrayList<>(List.of(Reflections.getClassInterfaces(targetType)));

        sourceCandidates.add(sourceType);
        targetCandidates.add(targetType);

        for (Class<?> sourceCandidate : sourceCandidates) {
            for (Class<?> targetCandidate : targetCandidates) {
                ClassPair<?, ?> pair = new ClassPair<>(sourceCandidate, targetCandidate);
                if (converters.containsKey(pair)) {
                    return (ClassPair<S, T>) pair;
                }
            }
        }

        return null;
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
            for (Edge<Class<?>> edge : Graph.toEdges(typePath)) {
                chain.add(new ClassPair<>(edge.nodeA(), edge.nodeB()));
            }
        }

        return chain;
    }
}
