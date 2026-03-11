package org.jmouse.action.adapter.el;

import org.jmouse.action.ActionDefinition;
import org.jmouse.action.ActionExecutor;
import org.jmouse.core.Verify;
import org.jmouse.core.scope.Context;
import org.jmouse.el.ExpressionLanguage;

/**
 * Adapter that executes actions defined as EL expressions. ⚙️
 *
 * <p>
 * Converts EL expressions such as:
 * </p>
 *
 * <pre>
 * {@code @Action[autoload]{'source':'user'}}
 * </pre>
 *
 * <p>
 * into {@link ActionDefinition} and delegates execution
 * to {@link ActionExecutor}.
 * </p>
 */
public class ActionExpressionAdapter {

    private final ExpressionLanguage el;
    private final ActionExecutor     executor;

    public ActionExpressionAdapter(ExpressionLanguage el, ActionExecutor executor) {
        this.el = Verify.nonNull(el, "el");
        this.executor = Verify.nonNull(executor, "executor");
    }

    /**
     * Executes the given EL action expression.
     */
    public <T> T execute(String expression, Context context) {
        Object result = el.evaluate(expression);

        if (!(result instanceof ActionDefinition definition)) {
            throw new IllegalArgumentException(
                    "Expression did not produce ActionDefinition: " + expression
            );
        }

        return executor.execute(definition, context);
    }
}