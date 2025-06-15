package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.util.Sorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

abstract public class AbstractHandlerMapping implements HandlerMapping {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractHandlerMapping.class);

    @Override
    public HandlerExecution getHandler(HttpServletRequest request) {
        Object handler = doGetHandler(request);

        if (handler == null) {
            return null;
        }

        HandlerExecution         execution    = new HandlerExecution(handler);
        List<HandlerInterceptor> interceptors = getHandlerInterceptors();

        if (interceptors != null && !interceptors.isEmpty()) {
            Sorter.sort(interceptors);
            interceptors.forEach(execution::addInterceptor);
        }

        return execution;
    }

    abstract protected Object doGetHandler(HttpServletRequest request);

    abstract protected List<HandlerInterceptor> getHandlerInterceptors();

}
