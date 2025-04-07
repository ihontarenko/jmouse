package org.jmouse.el.node;

import org.jmouse.el.evaluation.EvaluationContext;

/**
 * 🧩 Represents an expression node that can be evaluated within an {@link EvaluationContext}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ExpressionNode extends Node {

    /**
     * 🏗️ Evaluates the expression within the given context.
     *
     * @param context 🎛️ The evaluation context containing variables and functions.
     * @return 📤 The result of evaluating the expression.
     * @throws UnsupportedOperationException if evaluation is not implemented.
     */
    default Object evaluate(EvaluationContext context) {
        throw new UnsupportedOperationException(
                "Evaluation for expression node '%s' is not implemented"
                        .formatted(this.getClass().getName()));
    }

}
