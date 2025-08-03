package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.mvc.MvcContainer;
import org.jmouse.mvc.ReturnValueHandler;
import org.jmouse.web.context.WebBeanContext;

abstract public class AbstractReturnValueHandler implements ReturnValueHandler, InitializingBean {

    @Override
    public void handleReturnValue(MvcContainer mvcContainer, HttpServletRequest request, HttpServletResponse response) {

        doReturnValueHandle(mvcContainer, request, response);

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
            MvcContainer mvcContainer, HttpServletRequest request, HttpServletResponse response);

}
