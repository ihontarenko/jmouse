package org.jmouse.template;

public interface TemplateMaterializer<T> {
    T materialize(NodeTemplate template, RenderingExecution execution);
}
