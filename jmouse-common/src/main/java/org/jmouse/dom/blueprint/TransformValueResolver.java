package org.jmouse.dom.blueprint;

public final class TransformValueResolver implements ValueResolver {

    @Override
    public Object resolve(BlueprintValue value, RenderingExecution execution) {
        return switch (value) {
            case BlueprintValue.ConstantValue constant -> constant.value();
            case BlueprintValue.PathValue ignored -> null;
            case BlueprintValue.RequestAttributeValue ignored -> null;
            case null -> null;
        };
    }
}
