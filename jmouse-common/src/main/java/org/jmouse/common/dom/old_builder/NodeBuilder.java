package org.jmouse.common.dom.old_builder;

import org.jmouse.common.dom.Node;

public interface NodeBuilder<T> {
    Node build(T t, NodeBuilderContext ctx);
}
