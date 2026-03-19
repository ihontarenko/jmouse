package org.jmouse.core.mapping.strategy.direct;

import org.jmouse.core.Priority;
import org.jmouse.core.Verify;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.mapping.MappingContext;
import org.jmouse.core.mapping.strategy.MappingStrategy;
import org.jmouse.core.mapping.strategy.MappingStrategyContributor;
import org.jmouse.core.mapping.typed.TypeMapper;
import org.jmouse.core.reflection.InferredType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link MappingStrategyContributor} that resolves direct manual mappers by exact type pair. 🧭
 */
// higher priority MIN_VALUE
@Priority(Integer.MIN_VALUE)
public final class TypeMapperStrategyContributor implements MappingStrategyContributor {

    private final Map<Key, TypeMapper<?, ?>> mappers;

    public TypeMapperStrategyContributor(TypeMapper<?, ?>... mappers) {
        Map<Key, TypeMapper<?, ?>> index = new LinkedHashMap<>();

        for (TypeMapper<?, ?> mapper : mappers) {
            TypeMapper<?, ?> candidate = Verify.nonNull(mapper, "mapper");
            Key              key       = new Key(candidate.sourceType(), candidate.targetType());

            if (index.putIfAbsent(key, candidate) != null) {
                throw new IllegalArgumentException(
                        "Duplicate direct type mapper for source '%s' and target '%s'"
                                .formatted(candidate.sourceType().getName(), candidate.targetType().getName())
                );
            }
        }

        this.mappers = Map.copyOf(index);
    }

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        if (source == null) {
            return false;
        }

        Verify.nonNull(targetType, "targetType");

        Key key = new Key(source.getClass(), targetType.getClassType());

        return mappers.containsKey(key);
    }

    @Override
    public <T> MappingStrategy<T> build(TypedValue<T> typedValue, MappingContext context) {
        Object   source     = context.scope().sourceRoot();
        Class<?> sourceType = source != null ? source.getClass() : null;
        Class<?> targetType = typedValue.getType().getClassType();

        if (sourceType == null) {
            throw new IllegalStateException("Unable to resolve source type for direct type mapper strategy");
        }

        Key              key    = new Key(sourceType, targetType);
        TypeMapper<?, ?> mapper = mappers.get(key);

        if (mapper == null) {
            throw new IllegalStateException(
                    "No direct type mapper registered for source '%s' and target '%s'"
                            .formatted(sourceType.getName(), targetType.getName())
            );
        }

        return new DirectTypeMapperStrategy<>(mapper);
    }

    private record Key(Class<?> sourceType, Class<?> targetType) {
        @Override
        public String toString() {
            return "[%s:%s]".formatted(sourceType.getSimpleName(), targetType.getSimpleName());
        }
    }
}