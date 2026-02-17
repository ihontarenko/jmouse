package org.jmouse.dom.rendering;

import org.jmouse.dom.Node;

public abstract class AbstractRendererSupport {

    protected static String indentation(int depth) {
        return depth <= 0 ? "" : "  ".repeat(depth);
    }

    protected static String indentation(Node node) {
        return indentation(node.getDepth());
    }

}
