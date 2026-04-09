package org.jmouse.context.feature;

import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Constructor;

/**
 * 🏭 Reflection-based implementation of {@link FeatureSelectorFactory}.
 *
 * <p>
 * Creates {@link FeatureSelector} instances using reflection by locating
 * and invoking a suitable constructor.
 * </p>
 *
 * <p>
 * This implementation expects the selector class to have an accessible
 * constructor (typically a no-arg constructor). Instantiation is delegated
 * to {@link Reflections} utility methods.
 * </p>
 *
 * <p>
 * ⚠️ No caching is performed — a new instance is created on each invocation.
 * </p>
 */
public class ReflectionFeatureSelectorFactory implements FeatureSelectorFactory {

    /**
     * Creates a new instance of the given {@link FeatureSelector} type.
     *
     * <p>
     * Processing:
     * </p>
     * <ul>
     *     <li>locates the first available constructor</li>
     *     <li>instantiates it via {@link Reflections#instantiate}</li>
     * </ul>
     *
     * @param type selector implementation class
     * @return instantiated {@link FeatureSelector}
     * @throws IllegalStateException if instantiation fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public FeatureSelector create(Class<? extends FeatureSelector> type) {
        try {
            return Reflections.instantiate(
                    (Constructor<FeatureSelector>) Reflections.findFirstConstructor(type)
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to instantiate feature selector → type: " + type.getName(),
                    exception
            );
        }
    }

}