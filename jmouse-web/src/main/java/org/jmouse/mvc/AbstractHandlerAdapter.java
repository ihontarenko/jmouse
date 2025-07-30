package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.InitializingBean;
import org.jmouse.web.context.WebBeanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractHandlerAdapter implements HandlerAdapter, BeanContextAware, InitializingBean {

    private WebBeanContext context;

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractHandlerAdapter.class);

    @Override
    public HandlerResult handle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HandlerResult        handlerResponse = null;
        WebBeanContext context   = getContext(request);
        ModelContainer container = new ModelContainer();

        context.registerBean(Model.class, container.getModel(), BeanScope.REQUEST);

        container.markRequestUnhandled();

        Object returnValue = doHandle(request, response, handler);

        if (returnValue == null || response.isCommitted()) {
            container.markRequestHandled();

            if (!response.isCommitted()) {
                LOGGER.warn("Handler returns a null value and http-response is not flushed!");
            }

            if (returnValue != null) {
                returnValue = null;
                LOGGER.warn("Handler already written http-response. return value will ignored");
            }
        }

        return handlerResponse;
    }

    abstract protected Object doHandle(HttpServletRequest request, HttpServletResponse response, Object handler);

    public WebBeanContext getContext(HttpServletRequest request) {
        WebBeanContext context = (WebBeanContext) getBeanContext();

        if (context == null) {
            context = WebBeanContext.getRequiredWebBeanContext(request.getServletContext());
        }

        return context;
    }

    @Override
    public void afterCompletion(BeanContext context) {
        context.getBeans(ReturnValueHandler.class);
    }

    @Override
    public BeanContext getBeanContext() {
        return context;
    }

    @Override
    public void setBeanContext(BeanContext context) {
        this.context = (WebBeanContext) context;
    }

}
