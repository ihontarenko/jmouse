package org.jmouse.mvc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.web.context.WebBeanContext;

abstract public class AbstractExceptionResolver<T extends Throwable>
        implements ExceptionResolver<T>, InitializingBean {

    @Override
    public void resolveException(RequestContext requestContext, InvocationOutcome outcome, T exception) {
        doExceptionResolve(requestContext, outcome, exception);
    }

    @Override
    public void afterCompletion(BeanContext context) {
        initialize((WebBeanContext) context);
    }

    protected void initialize(WebBeanContext context) {
        doInitialize(context);
    }

    protected abstract void doInitialize(WebBeanContext context);

    protected abstract void doExceptionResolve(RequestContext requestContext, InvocationOutcome outcome, T exception);

}
