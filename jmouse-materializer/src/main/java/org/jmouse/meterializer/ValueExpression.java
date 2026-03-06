package org.jmouse.meterializer;

import org.jmouse.meterializer.build.AttributeMapBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                ValueExpression.OptionalValue,
                ValueExpression.RequestAttributeValue,
                ValueExpression.FormatValue,
                ValueExpression.MapValue,
                ValueExpression.ObjectValue {

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
     * Creates a root value expression.
     *
     * <p>
     * Represents the entire root model object.
     * Equivalent to a path that points to the root context.
     * </p>
     *
     * <pre>{@code
     * ValueExpression v = ValueExpression.root();
     * }</pre>
     *
     * @return root expression
     */
    static ValueExpression root() {
        return new ValueExpression.PathValue(null);
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
     * Creates an object-like expression resolved into Map<String, Object>. 🧩
     *
     * <pre>{@code
     * ValueExpression model = ValueExpression.object(
     *     "name", ValueExpression.format("%s.%s", path("name"), path("c.name")),
     *     "id", ValueExpression.format("%s_%s", path("name"), path("c.name")),
     *     "description", path("c.description")
     * );
     * }</pre>
     *
     * @param keyValues alternating key/value pairs: String, ValueExpression, ...
     * @return object expression
     */
    static ValueExpression object(Object... keyValues) {
        return new ValueExpression.ObjectValue(keyValues);
    }

    /**
     * Creates a {@link ValueExpression} representing a constant map value.
     *
     * @param map map value
     * @return map expression
     */
    static ValueExpression map(Map<String, Object> map) {
        return new MapValue(map);
    }

    /**
     * Wraps an expression as optional.
     *
     * <p>If the value cannot be resolved or resolves to {@code null},
     * the resolver may treat it as an absent value instead of failing.</p>
     *
     * @param optional wrapped expression
     * @return optional expression
     */
    static ValueExpression optional(ValueExpression optional) {
        return new ValueExpression.OptionalValue(optional);
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

    /**
     * {@link ValueExpression} representing a constant map value.
     *
     * <p>The map is returned as-is during value resolution.</p>
     */
    record MapValue(Map<String, Object> map) implements ValueExpression {
    }

    /**
     * {@link ValueExpression} representing an inline object definition.
     *
     * <p>
     * Accepts alternating key/value pairs:
     * <pre>{@code
     * new ObjectValue(
     *     "name", ValueExpression.constant("John"),
     *     "age",  ValueExpression.constant(30)
     * )
     * }</pre>
     *
     * <p>Keys must be {@link String} and values must be {@link ValueExpression}.</p>
     */
    record ObjectValue(Object... keyValues) implements ValueExpression {

        /**
         * Validates that key/value pairs are provided.
         */
        public ObjectValue {
            if (keyValues.length % 2 != 0) {
                throw new IllegalArgumentException("keyValues.length % 2 != 0");
            }
        }

        /**
         * Converts key/value pairs into an ordered map of entries.
         *
         * @return map of attribute name → value expression
         */
        public Map<String, ValueExpression> entries() {
            Map<String, ValueExpression> entries = new LinkedHashMap<>();

            for (int index = 0; index < keyValues.length; index += 2) {
                String key = (String) keyValues[index];
                ValueExpression value = (ValueExpression) keyValues[index + 1];
                entries.put(key, value);
            }

            return entries;
        }
    }

    /**
     * {@link ValueExpression} wrapper indicating that the inner expression is optional.
     *
     * <p>
     * If the wrapped value resolves to {@code null} or is missing, the resolver
     * may ignore the value instead of treating it as an error.
     * </p>
     */
    record OptionalValue(ValueExpression value) implements ValueExpression {
    }

}