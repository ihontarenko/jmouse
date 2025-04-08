package org.jmouse.el.node;

import org.jmouse.el.node.expression.ArgumentsNode;
import org.jmouse.el.node.expression.ArrayNode;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FunctionNode;

public interface Visitor {

    default void visit(Node node) {

    }

    default void visit(FunctionNode function) {

    }

    default void visit(ArgumentsNode arguments) {

    }

    default void visit(ArrayNode array) {

    }

    default void visit(BinaryOperation binary) {

    }

}
