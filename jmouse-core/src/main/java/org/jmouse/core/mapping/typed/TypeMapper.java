package org.jmouse.core.mapping.typed;

/**
 * Direct typed mapper for an exact source/target pair. 🧩
 *
 * @param <S> source type
 * @param <T> target type
 */
public interface TypeMapper<S, T> {

    /**
     * Source type supported by this mapper.
     */
    Class<S> sourceType();

    /**
     * Target type produced by this mapper.
     */
    Class<T> targetType();

    /**
     * Create and return a mapped target instance.
     */
    T map(S source);

    /**
     * Map into an existing target instance.
     *
     * <p>Default implementation is unsupported.</p>
     */
    default void map(S source, T target) {
        throw new UnsupportedOperationException("In-place mapping is not supported");
    }

    /**
     * Whether this mapper supports in-place mapping.
     */
    default boolean supportsInPlace() {
        return false;
    }
}