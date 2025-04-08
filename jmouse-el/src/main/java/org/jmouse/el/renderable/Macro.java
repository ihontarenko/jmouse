package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.FunctionNode;

public interface Macro {

    String name();

    Node node();

    String source();

    default void evaluate(Visitor visitor, FunctionNode node, EvaluationContext context) {

    }

}
