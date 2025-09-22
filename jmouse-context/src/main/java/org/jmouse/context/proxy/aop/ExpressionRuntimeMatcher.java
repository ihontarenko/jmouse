package org.jmouse.context.proxy.aop;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.ExpressionNode;

import java.lang.reflect.Method;

public class ExpressionRuntimeMatcher implements RuntimeMatcher {

    private final ExpressionLanguage el;
    private final ExpressionNode     compiled;

    public ExpressionRuntimeMatcher(ExpressionLanguage el, String expression) {
        this.el = el;
        this.compiled = el.compile(expression);
    }

    @Override
    public boolean runtimeMatches(Object proxy, Object target, Method method, Object[] arguments) {
        EvaluationContext context = el.newContext();

        context.setValue("proxy", proxy);
        context.setValue("target", target);
        context.setValue("method", method);
        context.setValue("arguments", arguments);
        context.setValue("class", target != null ? target.getClass() : null);

        return Boolean.parseBoolean(compiled.evaluate(context).toString());
    }

}
