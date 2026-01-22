package org.jmouse.core.mapping.factory;

/**
 * Creates target instances for mapping.
 *
 * @param <T> target type
 */
@FunctionalInterface
public interface ObjectFactory<T> {
    T create();
}
