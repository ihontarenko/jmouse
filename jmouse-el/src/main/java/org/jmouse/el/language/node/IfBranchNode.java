package org.jmouse.el.language.node;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.ExpressionsNode;

public class IfBranchNode extends ExpressionsNode {

    private Expression condition;

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public boolean isElse() {
        return condition == null;
    }

    public boolean matches(EvaluationContext context) {
        if (isElse()) {
            return true;
        }

        Conversion conversion = context.getConversion();

        return conversion.convert(condition.evaluate(context), Boolean.class);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object result = null;

        for (Expression expression : getExpressions()) {
            result = expression.evaluate(context);
        }

        return result;
    }
}