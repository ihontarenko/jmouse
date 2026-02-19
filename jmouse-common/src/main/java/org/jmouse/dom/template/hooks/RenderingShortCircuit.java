package org.jmouse.dom.template.hooks;

import org.jmouse.dom.Node;

/**
 * Allows a hook to short-circuit pipeline execution.
 */
public final class RenderingShortCircuit extends RuntimeException {

    private final Node result;

    public RenderingShortCircuit(Node result) {
        super("Rendering pipeline short-circuited.");
        this.result = result;
    }

    public Node result() {
        return result;
    }
}
