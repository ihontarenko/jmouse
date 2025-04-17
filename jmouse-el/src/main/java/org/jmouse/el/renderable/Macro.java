package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.FunctionNode;

public interface Macro extends Component {

    default void evaluate(Visitor visitor, FunctionNode node, EvaluationContext context) {

    }

}
