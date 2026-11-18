package org.jmouse.el.parser;

import org.jmouse.el.node.Node;

/** Reads one grammar production and returns its detached structural root. */
@FunctionalInterface
public interface ParseRule<I, N extends Node> {
    N parse(I input, ParseSession session);
}
