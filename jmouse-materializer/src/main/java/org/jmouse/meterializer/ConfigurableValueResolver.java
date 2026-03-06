package org.jmouse.meterializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default {@link ValueResolver} implementation with configurable resolution scope. 🧩
 *
 * <p>
 * Supports resolving the built-in {@link ValueExpression} variants:
 * </p>
 * <ul>
 *     <li>{@link ValueExpression.ConstantValue} — returned as-is</li>
 *     <li>{@link ValueExpression.PathValue} — resolved via {@link PathValueResolver}</li>
 *     <li>{@link ValueExpression.RequestAttributeValue} — read from {@link RenderingRequest#attributes()}</li>
 *     <li>{@link ValueExpression.FormatValue} — resolves arguments recursively and formats via {@link String#format}</li>
 * </ul>
 *
 * <p>
 * The {@link ResolutionMode} controls how aggressive the resolver is:
 * for example, in {@code CONSTANT_ONLY} mode non-constant expressions
 * are ignored and resolve to {@code null}.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * ValueResolver resolver = new ConfigurableValueResolver(
 *     ResolutionMode.FULL,
 *     (path, exec) -> exec.model().get(path) // example; your impl may differ
 * );
 *
 * Object v1 = resolver.resolve(ValueExpression.constant("ok"), execution);      // "ok"
 * Object v2 = resolver.resolve(ValueExpression.path("user.name"), execution);   // resolved by PathValueResolver
 * Object v3 = resolver.resolve(ValueExpression.request("locale"), execution);   // execution.request().attributes().get("locale")
 *
 * Object v4 = resolver.resolve(
 *     ValueExpression.format("Hello %s!", ValueExpression.path("user.name")),
 *     execution
 * );
 * }</pre>
 *
 * <p>
 * This resolver is stateless. Thread-safety depends on the provided
 * {@link PathValueResolver} implementation.
 * </p>
 */
public final class ConfigurableValueResolver implements ValueResolver {

    private final ResolutionMode    mode;
    private final PathValueResolver pathResolver;

    /**
     * Creates a configurable resolver.
     *
     * @param mode resolution mode controlling which expressions are evaluated
     * @param pathResolver path resolver used for {@link ValueExpression.PathValue}
     */
    public ConfigurableValueResolver(ResolutionMode mode, PathValueResolver pathResolver) {
        this.mode = mode;
        this.pathResolver = pathResolver;
    }

    /**
     * Resolves the given {@link ValueExpression} in the context of the current execution.
     *
     * <p>
     * For {@link ResolutionMode#CONSTANT_ONLY} only {@link ValueExpression.ConstantValue}
     * is resolved; other expressions return {@code null}.
     * </p>
     *
     * @param value expression to resolve
     * @param execution active rendering execution context
     * @return resolved value (may be {@code null})
     *
     * @throws IllegalStateException if expression type is unknown/unsupported
     */
    @Override
    public Object resolve(ValueExpression value, RenderingExecution execution) {
        if (value instanceof ValueExpression.ConstantValue(Object constant)) {
            return constant;
        }

        if (mode == ResolutionMode.CONSTANT_ONLY) {
            return null;
        }

        if (value instanceof ValueExpression.MapValue(Map<String, Object> map)) {
            return map;
        }

        if (value instanceof ValueExpression.OptionalValue(ValueExpression optional)) {
            try {
                return resolve(optional, execution);
            } catch (Exception e) {
                return null;
            }
        }

        if (value instanceof ValueExpression.PathValue(String path)) {
            return pathResolver.resolve(path, execution);
        }

        if (value instanceof ValueExpression.RequestAttributeValue(String name)) {
            return execution.request().attributes().get(name);
        }

        if (value instanceof ValueExpression.ObjectValue objectValue) {
            Map<String, Object> resolved = new LinkedHashMap<>();

            for (Map.Entry<String, ValueExpression> entry : objectValue.entries().entrySet()) {
                resolved.put(entry.getKey(), resolve(entry.getValue(), execution));
            }

            return resolved;
        }

        if (value instanceof ValueExpression.FormatValue(String pattern, List<ValueExpression> formatArguments)) {
            List<Object> arguments = new ArrayList<>();
            for (ValueExpression argument : formatArguments) {
                arguments.add(resolve(argument, execution));
            }
            return String.format(pattern, arguments.toArray());
        }

        throw new IllegalStateException("Unsupported value: " + value.getClass());
    }
}