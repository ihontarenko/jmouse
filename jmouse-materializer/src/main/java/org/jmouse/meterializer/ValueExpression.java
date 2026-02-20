package org.jmouse.meterializer;

import org.jmouse.meterializer.build.AttributeMapBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A blueprint value that can be either constant or bound to a data path.
 */
public sealed interface ValueExpression
        permits ValueExpression.ConstantValue,
                ValueExpression.PathValue,
                ValueExpression.RequestAttributeValue,
                ValueExpression.FormatValue {

    static ValueExpression constant(Object value) {
        return new ValueExpression.ConstantValue(value);
    }

    static ValueExpression path(String path) {
        return new ValueExpression.PathValue(path);
    }

    static ValueExpression format(String pattern, ValueExpression... arguments) {
        return new ValueExpression.FormatValue(pattern, List.of(arguments));
    }

    static ValueExpression request(String name) {
        return new ValueExpression.RequestAttributeValue(name);
    }

    static Map<String, ValueExpression> attributes(Consumer<AttributeMapBuilder> consumer) {
        AttributeMapBuilder builder = new AttributeMapBuilder();
        consumer.accept(builder);
        return builder.build();
    }

    /**
     * Constant value.
     *
     * @param value constant value
     */
    record ConstantValue(Object value) implements ValueExpression {
    }

    /**
     * Data-bound value resolved from a path.
     *
     * @param path path expression (for example "user.name")
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

    record FormatValue(String pattern, List<ValueExpression> arguments) implements ValueExpression {
    }


}