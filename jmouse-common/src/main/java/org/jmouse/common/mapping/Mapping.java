package org.jmouse.common.mapping;

import org.jmouse.core.reflection.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.jmouse.core.reflection.Reflections.getInterfacesParameterizedType;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * Interface for handling mapping operations between different structured types.
 * Provides methods to map objects, retrieve mappers, and register new mappers.
 */
public interface Mapping {

    Logger  LOGGER   = LoggerFactory.getLogger(Mapping.class);

    /**
     * Maps an structured to a target type.
     *
     * @param source the source structured to map
     * @param <R> the target type
     * @return the mapped structured of type R
     */
    <R> R map(Object source);

    /**
     * Retrieves a mapperProvider for the specified source structured.
     *
     * @param preferredType the source structured
     * @return the appropriate Mapper instance
     * @throws MappingException if no suitable mapperProvider is found
     */
    Set<Mapper<Object, Object>> mappers(Class<?> preferredType);

    /**
     * Retrieves a mapperProvider for the specified source structured.
     *
     * @param object the source structured
     * @param <S> the source type
     * @param <R> the target type
     * @return the appropriate Mapper instance
     * @throws MappingException if no suitable mapperProvider is found
     */
    <S, R> Mapper<S, R> mapper(S object);

    /**
     * Registers a new mapperProvider to handle specific structured mappings.
     *
     * @param mapper the Mapper to register
     */
    void register(Mapper<?, ?> mapper);

    /**
     * DirectAccess implementation of the Mapping interface.
     * Maintains a registry of mappers and handles mapping operations.
     */
    class DefaultMapping implements Mapping {

        private final Map<Class<?>, Set<Mapper<Object, Object>>> mappers = new HashMap<>();

        /**
         * Retrieves a mapperProvider for the specified source structured.
         *
         * @param source the source structured
         * @return the appropriate Mapper instance
         * @throws MappingException if no suitable mapperProvider is found
         */
        @Override
        public <S, R> Mapper<S, R> mapper(S source) {
            Class<?> expectedType = resolveExpectedType(source);

            for (Mapper<Object, Object> mapper : mappers(expectedType)) {
                if (mapper.supports(source)) {

                    LOGGER.info("Acceptable mapperProvider '{}' for type '{}'.",
                            getShortName(mapper), expectedType.getName());

                    return (Mapper<S, R>) mapper;
                }
            }

            throw new MappingException(
                    "No applicable mapperProvider was found for type '%s'".formatted(expectedType.getCanonicalName()));
        }

        /**
         * Retrieves a mapperProvider for the specified source structured.
         *
         * @param preferredType the source structured
         * @return the appropriate Mapper instance
         * @throws MappingException if no suitable mapperProvider is found
         */
        @Override
        public Set<Mapper<Object, Object>> mappers(Class<?> preferredType) {
            Set<Mapper<Object, Object>> candidates = mappers.get(preferredType);

            if (candidates == null) {
                throw new MappingException(
                        "No mapperProvider candidates was found passed source '%s'".formatted(getShortName(preferredType)));
            }

            return candidates;
        }

        /**
         * Maps an structured to a target type by using the appropriate mapperProvider.
         *
         * @param source the source structured to map
         * @param <R> the target type
         * @return the mapped structured of type R
         */
        @Override
        public <R> R map(Object source) {
            return this.<Object, R>mapper(source).map(source);
        }

        /**
         * Registers a new mapperProvider by determining its preferred type.
         *
         * @param mapper the Mapper to register
         * @throws MappingException if the mapperProvider type cannot be determined
         */
        @Override
        public void register(Mapper<?, ?> mapper) {
            Class<?> preferredType = resolvePreferredMappersType(mapper.getClass());

//            JavaType javaType      = JavaType.forInstance(mapperProvider);
//            Class<?> preferredType = javaType.locate(Mapper.class).getFirst().getRawType();

            if (preferredType == null) {
                throw new MappingException(
                        "Please generalize mapperProvider '%s' to resolve acceptable type".formatted(getShortName(mapper)));
            }

            LOGGER.info("Mapper '{}' assigned for type '{}'.", mapper.getClass().getName(), preferredType.getName());

            mappers.computeIfAbsent(preferredType, type -> new HashSet<>())
                    .add((Mapper<Object, Object>) mapper);
        }

        /**
         * Resolves the preferred type for the specified mapperProvider class.
         *
         * @param type the mapperProvider class
         * @return the preferred type, or null if not found
         */
        private Class<?> resolvePreferredMappersType(Class<?> type) {
            Class<?> preferredType = null;

            for (Class<?> superClass : Reflections.getSuperClasses(type)) {
                if((preferredType = getInterfacesParameterizedType(superClass, Mapper.class, 0)) != null) {
                    break;
                }
            }

            return preferredType;
        }

        /**
         * Resolves the expected type of the source structured for mapperProvider lookup.
         *
         * @param source the source structured
         * @return the resolved type
         */
        private Class<?> resolveExpectedType(Object source) {
            return mappers.containsKey(source.getClass()) ? source.getClass() : Object.class;
        }

    }
}
