package org.jmouse.security.authorization.method.expression;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.security.core.Authentication;

import java.lang.reflect.Method;

public class ELMethodExpressionHandler extends AbstractMethodExpressionHandler {

    protected ELMethodExpressionHandler(ExpressionLanguage language) {
        super(language);
    }

    @Override
    public EvaluationContext createContext(
            Authentication authentication, Method method, Object[] arguments, Object methodResult) {
        EvaluationContext evaluationContext = getExpressionLanguage().newContext();



        return evaluationContext;
    }

    @Override
    public boolean evaluate(String expression, EvaluationContext context) {
        return false;
    }

}
