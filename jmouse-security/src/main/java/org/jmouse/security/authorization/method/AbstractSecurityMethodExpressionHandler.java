package org.jmouse.security.authorization.method;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.MethodImporter;
import org.jmouse.security.core.access.AbstractExpressionHandler;
import org.jmouse.security.authorization.method.support.AuthenticationFunctions;
import org.jmouse.security.core.Authentication;

abstract public class AbstractSecurityMethodExpressionHandler<T> extends AbstractExpressionHandler<T> {

    public static final String VARIABLE_AUTHENTICATION = "authentication";

    public AbstractSecurityMethodExpressionHandler(ExpressionLanguage language) {
        super(language);
    }

    public AbstractSecurityMethodExpressionHandler() {
        super();
    }

    @Override
    public EvaluationContext createContext(Authentication authentication, T invocation) {
        EvaluationContext evaluationContext = getExpressionLanguage().newContext();
        evaluationContext.setValue(VARIABLE_AUTHENTICATION, authentication);
        attachAuthenticationFunction(authentication, evaluationContext);
        complementEvaluationContext(authentication, invocation, evaluationContext);
        return evaluationContext;
    }

    private void attachAuthenticationFunction(Authentication authentication, EvaluationContext evaluationContext) {
        MethodImporter.importMethod(
                new AuthenticationFunctions(authentication),
                AuthenticationFunctions.class,
                AuthenticationFunctions.NAMESPACE,
                evaluationContext
        );
    }

    abstract protected void complementEvaluationContext(
            Authentication authentication, T invocation, EvaluationContext context);

}
