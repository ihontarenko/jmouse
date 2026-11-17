package org.jmouse.mapper.strategy.optional;

import org.jmouse.core.access.TypedValue;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.strategy.MappingStrategy;

import java.util.Optional;

/**
 * 🎁 An {@link Optional} target: the source mapped into the type it holds and wrapped — a value already optional is unwrapped
 * first —; no source, or an empty one, is {@link Optional#empty()}, never {@code null}.
 *
 * <p>The type held is the target's type argument; a raw {@code Optional} holds the source as it is.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class OptionalStrategy<T> implements MappingStrategy<T> {

    @Override
    @SuppressWarnings("unchecked")
    public T execute(Object source, TypedValue<T> typedValue, MappingContext context) {
        Object held = source instanceof Optional<?> optional ? optional.orElse(null) : source;

        if (held == null) {
            return (T) Optional.empty();
        }

        InferredType heldType = typedValue.getType().getFirst();

        if (heldType == null || heldType.isUnresolved() || heldType.getClassType() == null) {
            return (T) Optional.of(held);
        }

        return (T) Optional.ofNullable(context.mapper().map(held, TypedValue.of(heldType)));
    }
}
