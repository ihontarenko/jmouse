package org.jmouse.core.access.accessor;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorProvider;
import org.jmouse.core.reflection.TypeInformation;

/**
 * An {@link ObjectAccessorProvider} implementation that supports Map-based data sources.
 * <p>
 * This valueProvider checks if the given source object is a Map using type information.
 * If the source is determined to be a Map, it creates and returns a corresponding
 * {@link ObjectAccessor} instance (specifically, a {@link MapAccessor}) that enables access
 * to the map's keys and values.
 * </p>
 */
public class MapAccessorProvider implements ObjectAccessorProvider {

    /**
     * Determines whether the provided source object is supported by this valueProvider.
     * <p>
     * This method uses {@link TypeInformation} to check if the source object is a Map.
     * </p>
     *
     * @param source the object to check
     * @return {@code true} if the source is a Map; {@code false} otherwise
     */
    @Override
    public boolean supports(Object source) {
        return TypeInformation.forInstance(source).isMap();
    }

    /**
     * Creates an {@link ObjectAccessor} instance for the given Map source.
     * <p>
     * If the source object is a Map, this method returns a new {@link MapAccessor} that
     * provides access to the map's entries.
     * </p>
     *
     * @param source the Map object to wrap
     * @return an {@link ObjectAccessor} instance wrapping the specified Map source
     */
    @Override
    public ObjectAccessor create(Object source) {
        return new MapAccessor(source);
    }
}
