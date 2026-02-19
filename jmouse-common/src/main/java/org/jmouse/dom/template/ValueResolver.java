package org.jmouse.dom.template;

@FunctionalInterface
public interface ValueResolver {
    Object resolve(ValueExpression value, RenderingExecution execution);
}
