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
    public MvcContainer handle(HttpServletRequest request, HttpServletResponse response, MappedHandler handler) {
        MvcContainer container = new MvcHandlerContainer(null);

        container.setState(ExecutionState.UNHANDLED);

        Object returnValue = doHandle(request, response, handler, container);

        container.setReturnValue(returnValue);

        getReturnValueProcessor()
                .process(container, request, response);

        return container;
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
            HttpServletRequest request, HttpServletResponse response, MappedHandler handler, MvcContainer mvcResult);

    @Override
    public void afterCompletion(BeanContext context) {
        setReturnValueHandlers(
                List.copyOf(context.getBeans(ReturnValueHandler.class))
        );
    }

}
