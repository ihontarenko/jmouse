package org.jmouse.context.proxy.aop;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.EvaluationException;
import org.jmouse.el.node.ExpressionNode;

import java.lang.reflect.Method;

public class ExpressionRuntimeMatcher implements RuntimeMatcher {

    private final ExpressionLanguage expressionLanguage;
    private final ExpressionNode     compiled;

    public ExpressionRuntimeMatcher(ExpressionLanguage expressionLanguage, String expression) {
        this.expressionLanguage = expressionLanguage;
        this.compiled = expressionLanguage.compile(expression);
    }

    @Override
    public boolean runtimeMatches(Object proxy, Object target, Method method, Object[] arguments) {
        EvaluationContext context = expressionLanguage.newContext();

        context.setValue("proxy", proxy);
        context.setValue("target", target);
        context.setValue("method", method);
        context.setValue("arguments", arguments);
        context.setValue("class", target != null ? target.getClass() : null);

        if (compiled.evaluate(context) instanceof Boolean booleanValue) {
            return booleanValue;
        }

        throw new EvaluationException("EVALUATED VALUE IS NOT BOOLEAN: " + compiled);
    }

}
