package org.jmouse.el.core.node;

import org.jmouse.el.renderable.node.*;

public interface NodeVisitor {

    default void visit(Node defaultNode) {
    }

    default void visit(RawTextNode textNode) {
    }

    default void visit(BlockNode blockNode) {
    }

    default void visit(MacroNode macroNode) {
    }

    default void visit(ExtendsNode extendsNode) {
    }

    default void visit(ImportNode importNode) {

    }

}
