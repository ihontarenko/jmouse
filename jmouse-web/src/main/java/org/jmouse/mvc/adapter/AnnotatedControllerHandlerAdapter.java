package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.method.HandlerMethod;
import org.jmouse.web.method.HandlerMethodContext;
import org.jmouse.web.method.HandlerMethodInvocation;
import org.jmouse.web.method.ReturnValueProcessor;
import org.jmouse.web.http.request.RequestContext;

/**
 * 🎬 HandlerAdapter implementation for controllers with annotated handler methods.
 *
 * <p>This adapter invokes the {@link HandlerMethod} using the configured
 * argument resolvers and sets the invocation result return value.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AnnotatedControllerHandlerAdapter extends AbstractHandlerAdapter {

    /**
     * Handles the HTTP request by invoking the handler method.
     *
     * @param request       the current HTTP request
     * @param response      the current HTTP response
     * @param mappedHandler the mapped handler containing the handler method and route info
     * @param outcome        the invocation outcome to populate with return value
     */
    @Override
    protected void doHandle(HttpServletRequest request, HttpServletResponse response, MappedHandler mappedHandler,
                            InvocationOutcome outcome) {
        HandlerMethod  handlerMethod  = (HandlerMethod) mappedHandler.handler();
        MappingResult  mappingResult  = mappedHandler.mappingResult();
        RequestContext requestContext = new RequestContext(request, response);

        HandlerMethodInvocation invocation = new HandlerMethodInvocation(
                new HandlerMethodContext(requestContext, handlerMethod), mappingResult, outcome, getArgumentResolvers());

        outcome.setReturnValue(invocation.invoke());
    }

    /**
     * Indicates whether this adapter supports the given handler.
     *
     * @param handler the mapped handler
     * @return {@code true} if the handler is a {@link HandlerMethod}, {@code false} otherwise
     */
    @Override
    public boolean supportsHandler(MappedHandler handler) {
        return handler.handler() instanceof HandlerMethod;
    }
}
