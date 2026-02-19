package org.jmouse.dom.blueprint;

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
            default -> throw new IllegalStateException("Unsupported blueprint value: " + value.getClass());
        };
    }
}

