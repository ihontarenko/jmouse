package org.jmouse.el.node;

import org.jmouse.el.node.expression.*;

public interface Visitor {

    default void visit(Node node) {

    }

    default void visit(ArgumentsNode arguments) {

    }

    default void visit(ArrayNode array) {

    }

    default void visit(BinaryOperation binary) {

    }

    default void visit(FilterNode filter) {

    }

    default void visit(FunctionNode function) {

    }

    default void visit(KeyValueNode keyValue) {

    }

    default void visit(LiteralNode<?> literal) {

    }

    default void visit(MapNode mapNode) {

    }

    default void visit(PropertyNode property) {

    }

    default void visit(ScopedCallNode scopedCall) {

    }

    default void visit(TestNode test) {

    }

    default void visit(UnaryOperation unary) {

    }

}
