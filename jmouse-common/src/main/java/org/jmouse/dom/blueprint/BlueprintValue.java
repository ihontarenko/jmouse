package org.jmouse.dom.blueprint;

import java.util.List;

/**
 * A blueprint value that can be either constant or bound to a data path.
 */
public sealed interface BlueprintValue
        permits BlueprintValue.ConstantValue,
                BlueprintValue.PathValue,
                BlueprintValue.RequestAttributeValue,
                BlueprintValue.FormatValue {

    /**
     * Constant value.
     *
     * @param value constant value
     */
    record ConstantValue(Object value) implements BlueprintValue {
    }

    /**
     * Data-bound value resolved from a path.
     *
     * @param path path expression (for example "user.name")
     */
    record PathValue(String path) implements BlueprintValue {
    }

    /**
     * Resolves value from {@link RenderingRequest#attributes()} by key.
     *
     * @param name request attribute name
     */
    record RequestAttributeValue(String name) implements BlueprintValue {}

    record FormatValue(String pattern, List<BlueprintValue> arguments) implements BlueprintValue {}


}