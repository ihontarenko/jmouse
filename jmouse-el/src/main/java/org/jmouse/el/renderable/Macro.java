package org.jmouse.el.renderable;

import org.jmouse.el.node.Node;

public interface Macro {

    String name();

    Node node();

    String source();

}
