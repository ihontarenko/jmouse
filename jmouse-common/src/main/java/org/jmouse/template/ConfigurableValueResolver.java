package org.jmouse.template;

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
    public Object resolve(ValueExpression value, RenderingExecution execution) {
        if (value instanceof ValueExpression.ConstantValue(Object constant)) {
            return constant;
        }

        if (mode == ResolutionMode.CONSTANT_ONLY) {
            return null;
        }

        if (value instanceof ValueExpression.PathValue(String path)) {
            return pathResolver.resolve(path, execution);
        }

        if (value instanceof ValueExpression.RequestAttributeValue(String name)) {
            return execution.request().attributes().get(name);
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

