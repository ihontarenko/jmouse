package org.jmouse.dom.old_builder;

import org.jmouse.dom.Node;

public interface NodeBuilder<T> {
    Node build(T t, NodeBuilderContext ctx);
}
