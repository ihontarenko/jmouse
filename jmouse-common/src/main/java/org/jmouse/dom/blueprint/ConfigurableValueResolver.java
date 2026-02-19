package org.jmouse.dom.blueprint;

import java.util.ArrayList;
import java.util.List;

public final class ConfigurableValueResolver implements ValueResolver {

    private final ResolutionMode    mode;
    private final PathValueResolver pathResolver;

    public ConfigurableValueResolver(ResolutionMode mode, PathValueResolver pathResolver) {
        this.mode = mode;
        this.pathResolver = pathResolver;
    }

    @Override
    public Object resolve(BlueprintValue value, RenderingExecution execution) {
        if (value instanceof BlueprintValue.ConstantValue(Object constant)) {
            return constant;
        }

        if (mode == ResolutionMode.CONSTANT_ONLY) {
            return null;
        }

        if (value instanceof BlueprintValue.PathValue(String path)) {
            return pathResolver.resolve(path, execution);
        }

        if (value instanceof BlueprintValue.RequestAttributeValue(String name)) {
            return execution.request().attributes().get(name);
        }

        if (value instanceof BlueprintValue.FormatValue(String pattern, List<BlueprintValue> formatArguments)) {
            List<Object> arguments = new ArrayList<>();
            for (BlueprintValue argument : formatArguments) {
                arguments.add(resolve(argument, execution));
            }
            return String.format(pattern, arguments.toArray());
        }

        throw new IllegalStateException("Unsupported value: " + value.getClass());
    }
}

