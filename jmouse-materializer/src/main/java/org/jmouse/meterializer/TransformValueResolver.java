package org.jmouse.meterializer;

/**
 * Minimal {@link ValueResolver} used during transformation phase. 🔄
 *
 * <p>
 * This resolver intentionally resolves only
 * {@link ValueExpression.ConstantValue} and ignores all other
 * expression types.
 * </p>
 *
 * <p>
 * It is typically used in pre-materialization stages where
 * dynamic resolution (paths, request attributes, formatting)
 * must be suppressed.
 * </p>
 *
 * <h3>Resolution rules</h3>
 *
 * <ul>
 *     <li>{@link ValueExpression.ConstantValue} → returned as-is</li>
 *     <li>{@link ValueExpression.PathValue} → {@code null}</li>
 *     <li>{@link ValueExpression.RequestAttributeValue} → {@code null}</li>
 *     <li>{@link ValueExpression.FormatValue} → {@code null}</li>
 *     <li>{@code null} → {@code null}</li>
 * </ul>
 *
 * <p>
 * This implementation is stateless and thread-safe.
 * </p>
 */
public final class TransformValueResolver implements ValueResolver {

    /**
     * Resolves the given value expression in transformation mode.
     *
     * <p>
     * Only constant expressions are resolved. All other types
     * return {@code null}.
     * </p>
     *
     * @param value expression to resolve
     * @param execution rendering execution context (ignored for non-constant expressions)
     * @return resolved constant value or {@code null}
     */
    @Override
    public Object resolve(ValueExpression value, RenderingExecution execution) {
        return switch (value) {
            case ValueExpression.ConstantValue constant -> constant.value();
            case ValueExpression.PathValue ignored -> null;
            case ValueExpression.RequestAttributeValue ignored -> null;
            case ValueExpression.FormatValue ignored -> null;
            case null -> null;
        };
    }
}