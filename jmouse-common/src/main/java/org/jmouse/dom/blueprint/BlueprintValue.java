package org.jmouse.dom.blueprint;

/**
 * A blueprint value that can be either constant or bound to a data path.
 */
public sealed interface BlueprintValue
        permits BlueprintValue.ConstantBlueprintValue,
                BlueprintValue.PathBlueprintValue,
                BlueprintValue.RequestAttributeBlueprintValue{

    /**
     * Constant value.
     *
     * @param value constant value
     */
    record ConstantBlueprintValue(Object value) implements BlueprintValue {
    }

    /**
     * Data-bound value resolved from a path.
     *
     * @param path path expression (for example "user.name")
     */
    record PathBlueprintValue(String path) implements BlueprintValue {
    }

    /**
     * Resolves value from {@link RenderingRequest#attributes()} by key.
     *
     * @param name request attribute name
     */
    record RequestAttributeBlueprintValue(String name) implements BlueprintValue {}

}