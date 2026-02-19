package org.jmouse.dom.template;

public final class TransformValueResolver implements ValueResolver {

    @Override
    public Object resolve(ValueExpression value, RenderingExecution execution) {
        return switch (value) {
            case ValueExpression.ConstantValue constant -> constant.value();
            case ValueExpression.PathValue ignored -> null;
            case ValueExpression.RequestAttributeValue ignored -> null;
            case ValueExpression.FormatValue ignored -> null;
            case null -> null;
        };
    }
}
