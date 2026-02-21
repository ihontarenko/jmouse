package org.jmouse.meterializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ValueResolver}. ⚙️
 *
 * <p>
 * Resolves all built-in {@link ValueExpression} variants without
 * additional resolution modes or restrictions.
 * </p>
 *
 * <ul>
 *     <li>{@link ValueExpression.ConstantValue} — returned as-is</li>
 *     <li>{@link ValueExpression.PathValue} — delegated to {@link PathValueResolver}</li>
 *     <li>{@link ValueExpression.RequestAttributeValue} — resolved from request attributes</li>
 *     <li>{@link ValueExpression.FormatValue} — arguments resolved recursively and formatted via {@link String#format}</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * ValueResolver resolver = new DefaultValueResolver(pathResolver);
 *
 * Object constant = resolver.resolve(
 *     ValueExpression.constant("Hello"),
 *     execution
 * );
 *
 * Object pathValue = resolver.resolve(
 *     ValueExpression.path("user.name"),
 *     execution
 * );
 *
 * Object formatted = resolver.resolve(
 *     ValueExpression.format("Hello %s", ValueExpression.path("user.name")),
 *     execution
 * );
 * }</pre>
 *
 * <p>
 * This implementation is stateless. Thread-safety depends on the provided
 * {@link PathValueResolver}.
 * </p>
 */
public final class DefaultValueResolver implements ValueResolver {

    private final PathValueResolver pathResolver;

    /**
     * Creates a resolver using the given {@link PathValueResolver}.
     *
     * @param pathResolver resolver used for path-based expressions
     */
    public DefaultValueResolver(PathValueResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    /**
     * Resolves the given {@link ValueExpression} within the current execution context.
     *
     * @param value expression to resolve
     * @param execution active rendering execution context
     * @return resolved value (may be {@code null})
     */
    @Override
    public Object resolve(ValueExpression value, RenderingExecution execution) {
        return switch (value) {
            case ValueExpression.ConstantValue constant -> constant.value();
            case ValueExpression.PathValue pathValue -> pathResolver.resolve(pathValue.path(), execution);
            case ValueExpression.RequestAttributeValue attributeValue ->
                    execution.request().attributes().get(attributeValue.name());
            case ValueExpression.FormatValue formatValue -> {
                List<Object> arguments = new ArrayList<>();
                for (ValueExpression argument : formatValue.arguments()) {
                    arguments.add(resolve(argument, execution));
                }
                yield String.format(formatValue.pattern(), arguments.toArray());
            }
        };
    }
}