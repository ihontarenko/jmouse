package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.util.Sorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

abstract public class AbstractHandlerAdapter implements HandlerAdapter, InitializingBean {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractHandlerAdapter.class);

    private List<ReturnValueHandler> returnValueHandlers = new ArrayList<>();

    @Override
    public ExecutionResult handle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ExecutionResult result = new HandlerExecutionResult(null);

        result.setState(ExecutionState.UNHANDLED);

        Object returnValue = doHandle(request, response, handler, result);

        result.setReturnValue(returnValue);

        getReturnValueProcessor()
                .process(result.getReturnValue(), request, response);

        if (returnValue == null || response.isCommitted()) {
            result.setState(ExecutionState.HANDLED);

            if (!response.isCommitted()) {
                LOGGER.warn("Handler return NULL value. HTTP-Response is not flushed!");
            }

            if (returnValue != null) {
                returnValue = null;
                LOGGER.warn("HTTP-Response is commited. Return value will ignored.");
            }
        }

        return result;
    }

    public ReturnValueProcessor getReturnValueProcessor() {
        List<ReturnValueHandler> returnValueHandlers = new ArrayList<>(getReturnValueHandlers());
        Sorter.sort(returnValueHandlers);
        return new ReturnValueProcessor(returnValueHandlers);
    }

    public List<ReturnValueHandler> getReturnValueHandlers() {
        return returnValueHandlers;
    }

    public void setReturnValueHandlers(List<ReturnValueHandler> returnValueHandlers) {
        this.returnValueHandlers = returnValueHandlers;
    }

    abstract protected Object doHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler, ExecutionResult mvcResult);

    @Override
    public void afterCompletion(BeanContext context) {
        setReturnValueHandlers(
                List.copyOf(context.getBeans(ReturnValueHandler.class))
        );
    }

}
