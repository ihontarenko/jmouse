package org.jmouse.el.translate;

import org.jmouse.el.node.Node;

@FunctionalInterface
public interface NodeOperation<N extends Node, C, R> {
    R apply(N node, C context);
}
