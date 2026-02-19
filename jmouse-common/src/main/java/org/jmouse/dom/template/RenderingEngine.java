package org.jmouse.dom.template;

import org.jmouse.dom.Node;

/**
 * Executes a rendering pipeline for a given blueprint key and model.
 */
@FunctionalInterface
public interface RenderingEngine {
    Node render(String blueprintKey, Object model, RenderingExecution parentExecution);
}
