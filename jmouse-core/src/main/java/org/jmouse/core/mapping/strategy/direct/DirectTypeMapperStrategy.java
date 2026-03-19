package org.jmouse.core.mapping.strategy.direct;

import org.jmouse.core.Verify;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.errors.ErrorCodes;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.typed.TypeMapper;

/**
 * {@link MappingStrategy} that delegates mapping to a registered {@link TypeMapper}. 🎯
 *
 * <p>This strategy is intended for exact source/target pairs where mapping is implemented
 * manually and should take precedence over reflective/property-based strategies.</p>
 *
 * @param <T> target type
 */
public final class DirectTypeMapperStrategy<T> implements MappingStrategy<T> {

    private final TypeMapper<Object, T> typeMapper;

    @SuppressWarnings("unchecked")
    public DirectTypeMapperStrategy(TypeMapper<?, ?> typeMapper) {
        this.typeMapper = (TypeMapper<Object, T>) Verify.nonNull(typeMapper, "typeMapper");
    }

    @Override
    public T execute(Object source, TypedValue<T> typedValue, MappingContext context) {
        Class<?> sourceType = source.getClass();
        Class<?> targetType = typedValue.getType().getClassType();

        try {
            T target = typedValue.getValue().get();

            if (target != null) {
                if (!typeMapper.supportsInPlace()) {
                    throw new MappingException(
                            ErrorCodes.STRATEGY_INCOMPATIBLE_TYPE,
                            "Direct mapper '%s' does not support in-place mapping for target type '%s'."
                                    .formatted(typeMapper.getClass().getName(), targetType.getName())
                    );
                }

                typeMapper.map(source, target);
                return target;
            }

            return typeMapper.map(source);
        } catch (MappingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MappingException(
                    ErrorCodes.STRATEGY_EXECUTION_FAILED,
                    "Direct mapper '%s' failed for source '%s' -> target '%s'."
                            .formatted(typeMapper.getClass().getName(), sourceType.getName(), targetType.getName()),
                    exception
            );
        }
    }
}