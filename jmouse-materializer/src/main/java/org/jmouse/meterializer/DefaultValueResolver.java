package org.jmouse.meterializer;

import java.util.ArrayList;
import java.util.List;

public final class DefaultValueResolver implements ValueResolver {

    private final PathValueResolver pathResolver;

    public DefaultValueResolver(PathValueResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    @Override
    public Object resolve(ValueExpression value, RenderingExecution execution) {
        return switch (value) {
            case ValueExpression.ConstantValue constant -> constant.value();
            case ValueExpression.PathValue pathValue -> pathResolver.resolve(pathValue.path(), execution);
            case ValueExpression.RequestAttributeValue attributeValue -> execution.request().attributes().get(attributeValue.name());
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

