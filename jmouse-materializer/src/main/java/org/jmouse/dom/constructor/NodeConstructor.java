package org.jmouse.dom.constructor;

import org.jmouse.dom.Node;

public interface NodeConstructor<T> {
    Node construct(T t, NodeConstructorContext context);
}
