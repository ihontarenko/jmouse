package org.jmouse.dom.blueprint;

@FunctionalInterface
public interface ValueResolver {
    Object resolve(BlueprintValue value, RenderingExecution execution);
}
