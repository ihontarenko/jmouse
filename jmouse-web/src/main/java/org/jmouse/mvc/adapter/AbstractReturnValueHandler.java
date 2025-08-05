package org.jmouse.mvc.adapter;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.RequestContext;
import org.jmouse.mvc.ReturnValueHandler;
import org.jmouse.web.context.WebBeanContext;

abstract public class AbstractReturnValueHandler implements ReturnValueHandler, InitializingBean {

    @Override
    public void handleReturnValue(InvocationOutcome result, RequestContext requestContext) {
        doReturnValueHandle(result, requestContext);
    }

    @Override
    public void afterCompletion(BeanContext context) {
        initialize((WebBeanContext) context);
    }

    protected void initialize(WebBeanContext context) {
        doInitialize(context);
    }

    protected abstract void doInitialize(WebBeanContext context);

    protected abstract void doReturnValueHandle(
            InvocationOutcome result, RequestContext requestContext);

}
