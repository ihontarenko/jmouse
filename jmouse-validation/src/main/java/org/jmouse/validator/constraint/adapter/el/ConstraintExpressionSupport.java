package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.ValidationDefinition;

public final class ConstraintExpressionSupport {

    private final ExpressionLanguage          expressionLanguage;
    private final ConstraintExpressionAdapter adapter;

    public ConstraintExpressionSupport(ExpressionLanguage expressionLanguage, ConstraintExpressionAdapter adapter) {
        this.expressionLanguage = expressionLanguage;
        this.adapter = adapter;
    }

    /**
     * Evaluate an EL expression like:
     *   "@MinMax('min':3, 'message':'out of range', 'mode':'min')"
     *
     * and map it into a typed {@link Constraint}.
     */
    public Constraint parse(String expression) {
        Object evaluated = expressionLanguage.evaluate(expression);

        if (!(evaluated instanceof ValidationDefinition definition)) {
            throw new IllegalArgumentException(
                    "Expression did not produce ValidationDefinition: " + expression
            );
        }

        return adapter.toConstraint(definition);
    }
}
