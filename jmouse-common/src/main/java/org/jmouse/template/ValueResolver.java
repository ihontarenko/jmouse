package org.jmouse.template;

@FunctionalInterface
public interface ValueResolver {
    Object resolve(ValueExpression value, RenderingExecution execution);
}
