package org.jmouse.template;

import org.jmouse.el.node.Node;
import org.jmouse.template.node.*;

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
