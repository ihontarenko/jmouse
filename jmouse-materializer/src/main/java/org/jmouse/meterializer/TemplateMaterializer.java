package org.jmouse.meterializer;

public interface TemplateMaterializer<T> {
    T materialize(NodeTemplate template, RenderingExecution execution);
}
