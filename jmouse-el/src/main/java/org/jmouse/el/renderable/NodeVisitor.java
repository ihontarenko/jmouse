package org.jmouse.el.renderable;

import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.node.*;

public interface NodeVisitor extends Visitor {

    default void visit(BlockNode blockNode) {
    }

    default void visit(ContainerNode containerNode) {
    }

    default void visit(ExtendsNode extendsNode) {
    }

    default void visit(RawTextNode textNode) {
    }

    default void visit(MacroNode macroNode) {
    }

    default void visit(ImportNode importNode) {
    }

    default void visit(IfNode ifNode) {
    }

}
