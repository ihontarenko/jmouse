package org.jmouse.mapper.strategy.scalar;

import org.jmouse.core.access.TypedValue;
import org.jmouse.mapper.errors.ErrorCodes;
import org.jmouse.mapper.errors.MappingException;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.support.AbstractStrategy;
import org.jmouse.mapper.MappingContext;
import org.jmouse.core.reflection.TypeClassifier;

/**
 * Mapping strategy for scalar-like targets (primitive wrappers, strings, enums, and plain classes). 🧱
 *
 * <p>{@code ScalarStrategy} performs minimal mapping logic:</p>
 * <ul>
 *   <li>if the target type is scalar/enum/class, it attempts conversion via {@link org.jmouse.core.convert.Conversion}</li>
 *   <li>otherwise it returns the source value as-is (cast)</li>
 * </ul>
 *
 * <p>This strategy is intended for fast-path mapping where deep object traversal is not required.</p>
 *
 * @param <T> target type produced by the strategy
 */
public final class ScalarStrategy<T> extends AbstractStrategy<T> implements MappingStrategy<T> {

    /**
     * Execute scalar mapping.
     *
     * <p>If {@code source} is {@code null}, returns {@code null}.</p>
     *
     * @param source source object
     * @param context mapping context providing conversion service
     * @return mapped scalar value
     */
    @Override
    public T execute(Object source, TypedValue<T> typedValue, MappingContext context) {
        if (source == null) {
            return null;
        }

        TypeClassifier typeClassifier = typedValue.getType();

        if (typeClassifier.isScalar() || typeClassifier.isEnum() || typeClassifier.isClass()) {
            try {
                @SuppressWarnings("unchecked")
                T converted = (T) convertIfNeeded(source, typeClassifier.getClassType(), context.conversion());
                return converted;
            } catch (Exception exception) {
                throw toMappingException(
                        context,
                        ErrorCodes.SCALAR_CONVERSION_FAILED,
                        "Conversion of scalar value to '%s' failed.".formatted(typedValue.getType()),
                        exception
                );
            }
        }

        @SuppressWarnings("unchecked")
        T casted = (T) source;

        return casted;
    }
}
