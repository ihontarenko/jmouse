package org.jmouse.dom.meterializer.rules;

import org.jmouse.dom.Node;
import org.jmouse.meterializer.RenderingExecution;

public interface NodeRule {

    int order();

    boolean matches(Node node, RenderingExecution execution);

    void apply(Node node, RenderingExecution execution);

}
