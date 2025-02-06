package org.jmouse.common.dom.builder;

import org.jmouse.common.dom.Node;

public interface NodeBuilder<T> {
    Node build(T t, NodeBuilderContext ctx);
}
