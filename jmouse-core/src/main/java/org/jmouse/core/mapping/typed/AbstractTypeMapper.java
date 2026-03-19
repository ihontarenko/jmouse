package org.jmouse.core.mapping.typed;

import org.jmouse.core.Verify;

/**
 * Base class for {@link TypeMapper} with fixed source/target types. 🧱
 *
 * @param <S> source type
 * @param <T> target type
 */
public abstract class AbstractTypeMapper<S, T> implements TypeMapper<S, T> {

    private final Class<S> sourceType;
    private final Class<T> targetType;

    protected AbstractTypeMapper(Class<S> sourceType, Class<T> targetType) {
        this.sourceType = Verify.nonNull(sourceType, "sourceType");
        this.targetType = Verify.nonNull(targetType, "targetType");
    }

    @Override
    public final Class<S> sourceType() {
        return sourceType;
    }

    @Override
    public final Class<T> targetType() {
        return targetType;
    }
}