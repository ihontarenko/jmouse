package org.jmouse.dom.blueprint;

import java.util.ArrayList;
import java.util.List;

public final class DefaultValueResolver implements ValueResolver {

    private final PathValueResolver pathResolver;

    public DefaultValueResolver(PathValueResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    @Override
    public Object resolve(BlueprintValue value, RenderingExecution execution) {
        return switch (value) {
            case BlueprintValue.ConstantValue constant -> constant.value();
            case BlueprintValue.PathValue pathValue -> pathResolver.resolve(pathValue.path(), execution);
            case BlueprintValue.RequestAttributeValue attributeValue -> execution.request().attributes().get(attributeValue.name());
            case BlueprintValue.FormatValue formatValue -> {
                List<Object> arguments = new ArrayList<>();
                for (BlueprintValue argument : formatValue.arguments()) {
                    arguments.add(resolve(argument, execution));
                }
                yield String.format(formatValue.pattern(), arguments.toArray());
            }
        };
    }
}

