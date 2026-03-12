package org.jmouse.action.adapter.el;

import org.jmouse.action.ActionDefinition;
import org.jmouse.action.ActionExecutor;
import org.jmouse.core.scope.Context;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;

import static org.jmouse.core.Verify.nonNull;

/**
 * Adapter that executes actions defined as EL expressions. ⚙️
 *
 * <p>
 * Converts EL expressions such as:
 * </p>
 *
 * <pre>
 * {@code @[scope:autoload]{'source':'user'}}
 * </pre>
 *
 * <p>
 * into {@link ActionDefinition} and delegates execution
 * to {@link ActionExecutor}.
 * </p>
 */
public class ActionExpressionAdapter {

    private final ExpressionLanguage expressionLanguage;
    private final ActionExecutor     actionExecutor;

    public ActionExpressionAdapter(ExpressionLanguage expressionLanguage, ActionExecutor actionExecutor) {
        this.expressionLanguage = nonNull(expressionLanguage, "el");
        this.actionExecutor = nonNull(actionExecutor, "executor");
    }

    /**
     * Executes the given EL action expression.
     */
    public <T> T execute(String expression, Context context) {
        EvaluationContext evaluationContext = expressionLanguage.newContext();
        context.getProperties().forEach((k, value) -> evaluationContext.setValue((String) k, value));
        Object result = expressionLanguage.evaluate(expression, evaluationContext);

        if (!(result instanceof ActionDefinition definition)) {
            throw new IllegalArgumentException(
                    "Expression did not produce ActionDefinition: " + expression
            );
        }

        return actionExecutor.execute(definition, context);
    }
}