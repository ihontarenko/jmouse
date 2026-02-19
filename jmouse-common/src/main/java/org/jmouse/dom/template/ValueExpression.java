package org.jmouse.dom.template;

import java.util.List;

/**
 * A blueprint value that can be either constant or bound to a data path.
 */
public sealed interface ValueExpression
        permits ValueExpression.ConstantValue,
                ValueExpression.PathValue,
                ValueExpression.RequestAttributeValue,
                ValueExpression.FormatValue {

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
    record RequestAttributeValue(String name) implements ValueExpression {}

    record FormatValue(String pattern, List<ValueExpression> arguments) implements ValueExpression {}


}