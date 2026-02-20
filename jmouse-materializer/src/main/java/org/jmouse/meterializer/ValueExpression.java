package org.jmouse.meterializer;

import org.jmouse.meterializer.build.AttributeMapBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Declarative value expression used during rendering. 🧩
 *
 * <p>
 * Represents a blueprint-level value that is resolved at runtime
 * by a {@code ValueResolver}. A value expression may be:
 * </p>
 *
 * <ul>
 *     <li>constant</li>
 *     <li>resolved from a data path</li>
 *     <li>resolved from request attributes</li>
 *     <li>formatted from multiple nested expressions</li>
 * </ul>
 *
 * <h3>Examples</h3>
 *
 * <pre>{@code
 * ValueExpression v1 = ValueExpression.constant("Hello");
 *
 * ValueExpression v2 = ValueExpression.path("user.name");
 *
 * ValueExpression v3 = ValueExpression.format(
 *     "Hello %s!",
 *     ValueExpression.path("user.name")
 * );
 *
 * ValueExpression v4 = ValueExpression.request("locale");
 * }</pre>
 *
 * <p>
 * Instances are immutable and form a small expression tree.
 * Actual resolution semantics are delegated to the rendering engine.
 * </p>
 */
public sealed interface ValueExpression
        permits ValueExpression.ConstantValue,
        ValueExpression.PathValue,
        ValueExpression.RequestAttributeValue,
        ValueExpression.FormatValue {

    /**
     * Creates a constant value expression.
     *
     * @param value constant value (may be {@code null})
     * @return constant expression
     */
    static ValueExpression constant(Object value) {
        return new ValueExpression.ConstantValue(value);
    }

    /**
     * Creates a path-based value expression.
     *
     * @param path data path (e.g. {@code "user.name"})
     * @return path expression
     */
    static ValueExpression path(String path) {
        return new ValueExpression.PathValue(path);
    }

    /**
     * Creates a formatted value expression.
     *
     * <p>
     * Pattern semantics are implementation-defined
     * (typically {@link String#format(String, Object...)}-like).
     * Arguments are resolved before formatting.
     * </p>
     *
     * @param pattern   format pattern
     * @param arguments nested expressions
     * @return format expression
     */
    static ValueExpression format(String pattern, ValueExpression... arguments) {
        return new ValueExpression.FormatValue(pattern, List.of(arguments));
    }

    /**
     * Creates an expression that resolves a request attribute.
     *
     * @param name attribute name
     * @return request-bound expression
     */
    static ValueExpression request(String name) {
        return new ValueExpression.RequestAttributeValue(name);
    }

    /**
     * Utility method to build attribute maps fluently. 🏗️
     *
     * <pre>{@code
     * Map<String, ValueExpression> attrs =
     *     ValueExpression.attributes(a -> a
     *         .put("class", ValueExpression.constant("btn"))
     *         .put("title", ValueExpression.path("user.name"))
     *     );
     * }</pre>
     *
     * @param consumer builder customization callback
     * @return immutable attribute map
     */
    static Map<String, ValueExpression> attributes(Consumer<AttributeMapBuilder> consumer) {
        AttributeMapBuilder builder = new AttributeMapBuilder();
        consumer.accept(builder);
        return builder.build();
    }

    /**
     * Constant value expression.
     *
     * @param value constant value
     */
    record ConstantValue(Object value) implements ValueExpression {
    }

    /**
     * Data-bound expression resolved from a path.
     *
     * <p>Example: {@code "user.name"}.</p>
     *
     * @param path path expression
     */
    record PathValue(String path) implements ValueExpression {
    }

    /**
     * Resolves value from {@link RenderingRequest#attributes()} by key.
     *
     * @param name request attribute name
     */
    record RequestAttributeValue(String name) implements ValueExpression {
    }

    /**
     * Composite formatted expression.
     *
     * <p>
     * The pattern is applied after all argument expressions
     * are resolved.
     * </p>
     *
     * @param pattern   formatting pattern
     * @param arguments nested value expressions
     */
    record FormatValue(String pattern, List<ValueExpression> arguments) implements ValueExpression {
    }

}