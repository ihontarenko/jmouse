package org.jmouse.core.convert;

import org.jmouse.core.graph.*;
import org.jmouse.core.reflection.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
public class StandardConversion implements Conversion {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardConversion.class.getPackageName() + ".CONVERTER");

    private final Map<ClassPair, GenericConverter<?, ?>> converters = new ConcurrentHashMap<>();
    private final TypeNormalizer                         normalizer = new TypeNormalizer.EnumTypeNormalizer();
    private final Graph<Class<?>>                        graph      = new DirectedMapGraph<>();
    private final PathFinder<Class<?>>                   pathFinder = new BFSPathFinder<>();

    /**
     * Memoized answers to {@link #hasAnyConverter(Class, Class)}.
     *
     * <p>⚠️ That question is asked once per property of every mapped object, and answering it in the
     * negative costs a breadth-first search over the whole type graph. Registering a converter
     * invalidates this, because a pair that had no path may now have one.</p>
     */
    private final Map<ClassPair, Boolean> reachable = new ConcurrentHashMap<>();

    /**
     * Converters reachable by name.
     *
     * <p>⚠️ <strong>Deliberately a second registry rather than an index over the first.</strong> A name
     * and a type pair answer different questions: the pair asks <em>how do I get from here to there</em>
     * and must have one answer, while a name asks for <em>this particular</em> transformation and exists
     * precisely because one pair may have several. Indexing the pair registry by name would make every
     * named converter also answer the generic question, which is the failure names were added to
     * prevent.</p>
     */
    private final Map<String, GenericConverter<?, ?>> named = new ConcurrentHashMap<>();

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
        for (ClassPair supportedType : genericConverter.getSupportedTypes()) {
            graph.addEdge(supportedType.classA(), supportedType.classB());
            converters.putIfAbsent(supportedType, genericConverter);
        }

        reachable.clear();
    }

    /**
     * Registers a converter under a name only — see
     * {@link ConverterFactory#registerConverter(String, Class, Class, Converter)}.
     *
     * @param <S>        the source type
     * @param <T>        the target type
     * @param name       what to call it
     * @param sourceType the source class
     * @param targetType the target class
     * @param converter  the converter instance
     */
    @Override
    public <S, T> void registerConverter(
            String name, Class<S> sourceType, Class<T> targetType, Converter<S, T> converter) {
        registerConverter(name, GenericConverter.of(sourceType, targetType, converter));
    }

    /**
     * Registers a {@link GenericConverter} under a name only.
     *
     * <p>⚠️ It is not added to the pair registry and not added to the type graph, so it changes no
     * answer any existing caller gets. That is the point: a named converter is reached deliberately or
     * not at all.</p>
     *
     * @param name             what to call it
     * @param genericConverter the converter
     */
    @Override
    public void registerConverter(String name, GenericConverter<?, ?> genericConverter) {
        String claimed = ConverterName.of(name).name();

        GenericConverter<?, ?> existing = named.putIfAbsent(claimed, genericConverter);

        if (existing != null && existing != genericConverter) {
            throw new IllegalArgumentException(
                    ("the name '%s' is already registered to %s; two converters under one name is the "
                     + "ambiguity naming them was meant to remove")
                            .formatted(claimed, existing.getClass().getName()));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, T> GenericConverter<S, T> getConverter(String name) {
        return (GenericConverter<S, T>) named.get(name);
    }

    /**
     * ⚠️ Sorted, because this list ends up inside a refusal message. A set iterated in hash order
     * reads differently on two runs, which makes a message somebody is comparing look like it changed.
     */
    @Override
    public Set<String> converterNames() {
        return new TreeSet<>(named.keySet());
    }

    /**
     * Check whether any conversion path exists between {@code sourceType} and {@code targetType}.
     *
     * <p>This method first checks for a direct converter. If none is found, it tries to resolve
     * an indirect conversion via:</p>
     * <ul>
     *   <li>a compatible converter candidate (e.g. assignable source/target match), or</li>
     *   <li>a multi-step transition chain (A -&gt; B -&gt; C) that can reach {@code targetType}</li>
     * </ul>
     *
     * @param sourceType source runtime type (never {@code null})
     * @param targetType target runtime type (never {@code null})
     * @return {@code true} if a direct converter, candidate converter, or transition chain exists;
     *         {@code false} otherwise
     */
    @Override
    public boolean hasAnyConverter(Class<?> sourceType, Class<?> targetType) {
        ClassPair pair  = new ClassPair(sourceType, targetType);
        Boolean   known = reachable.get(pair);

        // ⚠️ Read before written to, because this is asked once per property of every mapped object from
        // a memo that has been fully populated since the first object went through. computeIfAbsent
        // builds its mapping function whether or not it is needed - the lambda below captures `this`, so
        // it was an allocation on every hit, to answer a question the table already knew. The extra get
        // on a miss is paid once per pair, in front of a breadth-first search over the type graph.
        if (known != null) {
            return known;
        }

        return reachable.computeIfAbsent(
                pair, absent -> searchAnyConverter(absent.classA(), absent.classB()));
    }

    /**
     * Answers whether any conversion path exists, without consulting the memo.
     *
     * @param sourceType source runtime type
     * @param targetType target runtime type
     * @return {@code true} if a direct converter, a candidate, or a transition chain exists
     */
    private boolean searchAnyConverter(Class<?> sourceType, Class<?> targetType) {
        if (hasConverter(sourceType, targetType)) {
            return true;
        }

        if (searchPossibleCandidate(sourceType, targetType) != null) {
            return true;
        }

        List<ClassPair> chain = searchTransitionChain(sourceType, targetType);

        return chain != null && !chain.isEmpty();
    }

    /**
     * Checks if a converter exists for the specified {@link ClassPair}.
     *
     * @param classPair a {@link ClassPair} representing the source and target types
     * @return {@code true} if a converter exists, {@code false} otherwise
     */
    @Override
    public boolean hasConverter(ClassPair classPair) {
        if (converters.containsKey(classPair)) {
            return true;
        }

        // ⚠️ The exact pair is asked a second time in here, and that is left alone deliberately.
        // searchPossibleCandidate's candidate lists both begin with the types themselves, so its first
        // probe repeats the line above - but the only way to remove it is to teach the search which pair
        // its caller already ruled out, and this whole branch sits behind the `reachable` memo and is
        // reached once per pair for the life of the conversion. A cold repeat is not worth a search that
        // takes an argument nobody else can supply correctly.
        return searchPossibleCandidate(classPair.classA(), classPair.classB()) != null;
    }

    /**
     * Removes a registered converter for the specified source and target types.
     *
     * @param classPair a {@link ClassPair} representing the source and target types
     * @return {@code true} if a converter was removed, {@code false} if no such converter was found
     */
    @Override
    public boolean removeConverter(ClassPair classPair) {
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
    public <S, T> GenericConverter<S, T> getConverter(ClassPair classPair) {
        if (!hasConverter(classPair)) {
            throw new ConverterNotFound(classPair);
        }
        return (GenericConverter<S, T>) converters.get(classPair);
    }

    /**
     * Attempts to locate a {@link GenericConverter} by exploring compatible
     * source and target types based on the provided {@link ClassPair}.
     * <p>
     * This method delegates to {@link #getConverter(ClassPair)} after
     * identifying a concrete pair of types through {@code searchPossibleCandidate}.
     * It is commonly invoked by higher‐level lookup methods that accept
     * source and target classes separately.
     * </p>
     *
     * @param <S>       the source type parameter
     * @param <T>       the target type parameter
     * @param classPair a {@link ClassPair} encapsulating the source and
     *                  target types to search for
     * @return a compatible {@link GenericConverter} if found, or {@code null}
     */
    @Override
    public <S, T> GenericConverter<S, T> findConverter(ClassPair classPair) {
        ClassPair candidate = searchPossibleCandidate(classPair.classA(), classPair.classB());

        if (candidate == null) {
            throw new ConverterNotFound(classPair);
        }

        return getConverter(candidate);
    }

    /**
     * Converts the given {@code source} structured from type {@code T} to type {@code R} using a registered converter.
     * <p>
     * The method first looks up a converter based on the source and target types. If a converter is found,
     * it performs the conversion. If no direct converter is available, the method attempts to find a suitable converter
     * through inherited types or by searching for a transition chain using a graph of type converters. If no converter
     * is found, a {@link ConverterNotFound} exception is thrown.
     * </p>
     *
     * @param <T>        the type of the source structured
     * @param <R>        the type of the desired result
     * @param source     the structured to convert
     * @param sourceType the class representing the source type
     * @param targetType the class representing the target type
     * @return the converted structured of type {@code R}, or {@code null} if the source is {@code null}
     * @throws ConverterNotFound if no converter is registered for the given source and target types
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public <T, R> R convert(T source, Class<T> sourceType, Class<R> targetType) {
        R converted = null;

        if (targetType == Object.class) {
            converted = (R) source;
        } else if (source != null) {
            @SuppressWarnings({"unchecked"}) Class<R> normalizedType = (Class<R>) normalizer.normalize(targetType);
            ClassPair classPair = new ClassPair(sourceType, normalizedType);

            if (classPair.isTheSame()) {
                return (R) source;
            }

            GenericConverter<T, R> converter = null;

            try {
                // Try to find a direct converter
                converter = getConverter(classPair);
            } catch (ConverterNotFound ignored) {}

            // Try to find a enum-specific converter if any
            if (Enum.class.isAssignableFrom(targetType)) {
                GenericConverter<T, R> enumSpecific = getConverter(ClassPair.of(sourceType, targetType));
                if (enumSpecific != null) {
                    converter = enumSpecific;
                }
            }

            if (converter == null) {
                // Attempt to find a converter using inherited types
                ClassPair candidate = searchPossibleCandidate(sourceType, targetType);

                if (candidate != null) {
                    classPair = candidate;
                    converter = getConverter(classPair);
                }
            }

            if (converter == null) {
                // Search for a transition chain using a graph of converters and BFS search
                List<ClassPair> transitions = searchTransitionChain(sourceType, targetType);

                if (transitions.isEmpty()) {
                    throw new ConverterNotFound(classPair);
                }

                LOGGER.debug("Conversion transitions: {}", transitions);

                Object intermediate = source;

                for (ClassPair transition : transitions) {
                    intermediate = getConverter(transition).convert(intermediate, (Class<Object>) transition.classB());
                }

                converted = (R) intermediate;
            } else {
                LOGGER.debug("Converter: {}", classPair);
                converted = converter.convert(source, targetType);
            }

        }

        return converted;
    }

    /**
     * Searches for a potential converter candidate by checking interfaces and superclasses of the source and target types.
     * But not optimized operation with O(n^2) complexity
     * <p>
     * The method iterates over the interfaces and classes of the source and target types to find a {@link ClassPair}
     * that matches a registered converter.
     * </p>
     *
     * @param <S>        the source type
     * @param <T>        the target type
     * @param sourceType the class representing the source type
     * @param targetType the class representing the target type
     * @return a {@link ClassPair} containing the source and target types if a suitable converter is found,
     * {@code null} if no converter is found
     */
    public <S, T> ClassPair searchPossibleCandidate(Class<S> sourceType, Class<T> targetType) {
        List<Class<?>> sourceCandidates = new ArrayList<>(List.of(Reflections.getClassInterfaces(sourceType)));
        List<Class<?>> targetCandidates = new ArrayList<>(List.of(Reflections.getClassInterfaces(targetType)));

        sourceCandidates.add(sourceType);
        sourceCandidates.add(sourceType.getSuperclass());
        targetCandidates.add(targetType);

        if (targetType.isEnum()) {
            targetCandidates.add(Enum.class);
        }

        for (Class<?> sourceCandidate : sourceCandidates) {
            for (Class<?> targetCandidate : targetCandidates) {
                ClassPair pair = new ClassPair(sourceCandidate, targetCandidate);
                if (converters.containsKey(pair)) {
                    return pair;
                }
            }
        }

        if (sourceType.isArray() && targetType.isArray()) {
            ClassPair objectsToObjects = ClassPair.of(Object[].class, Object[].class);
            if (converters.containsKey(objectsToObjects)) {
                return objectsToObjects;
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
     * @param <S>        the initial source type
     * @param <T>        the final target type
     * @param sourceType the class representing the starting type of the conversion
     * @param targetType the class representing the desired final type of the conversion
     * @return a list of {@link GenericConverter} instances representing the conversion path
     * from {@code sourceType} to {@code targetType}, or an empty list if no such path exists
     */
    @Override
    public <S, T> List<ClassPair> searchTransitionChain(Class<S> sourceType, Class<T> targetType) {
        List<Class<?>>  typePath = pathFinder.findPath(graph, sourceType, targetType);
        List<ClassPair> chain    = new ArrayList<>();

        if (!typePath.isEmpty()) {
            for (Edge<Class<?>> edge : Graph.toEdges(typePath)) {
                chain.add(new ClassPair(edge.nodeA(), edge.nodeB()));
            }
        }

        return chain;
    }
}
