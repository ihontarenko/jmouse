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

    default void visit(IfNode ifNode) {
    }

    default void visit(ImportNode importNode) {
    }

    default void visit(IncludeNode includeNode) {
    }

    default void visit(MacroNode macroNode) {
    }

    default void visit(PrintNode printNode) {
    }

    default void visit(TextNode textNode) {
    }

}
