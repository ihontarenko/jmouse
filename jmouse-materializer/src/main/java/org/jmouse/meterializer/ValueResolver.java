package org.jmouse.meterializer;

@FunctionalInterface
public interface ValueResolver {
    Object resolve(ValueExpression value, RenderingExecution execution);
}
