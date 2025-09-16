package org.jmouse.web.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MethodParameter;
import org.jmouse.core.Sorter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.RequestAttributes;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.mvc.method.ArgumentResolver;
import org.jmouse.web.mvc.method.HandlerMethod;
import org.jmouse.web.mvc.method.HandlerMethodContext;
import org.jmouse.web.mvc.method.HandlerMethodInvocation;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.mvc.AbstractHandlerMethodAdapter;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.mvc.MappedHandler;
import org.jmouse.web.mvc.MappingResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 🎬 HandlerAdapter implementation for controllers with annotated handler methods.
 *
 * <p>This adapter invokes the {@link HandlerMethod} using the configured
 * argument resolvers and sets the invocation result return value.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AnnotatedControllerHandlerMethodAdapter extends AbstractHandlerMethodAdapter {

    /** Resolvers for handler method arguments. */
    private List<ArgumentResolver> argumentResolvers;

    /**
     * Handles the HTTP request by invoking the handler method.
     *
     * @param request       the current HTTP request
     * @param response      the current HTTP response
     * @param mappedHandler the mapped handler containing the handler method and route info
     */
    @Override
    protected MVCResult doInvokeHandler(
            HttpServletRequest request, HttpServletResponse response, MappedHandler mappedHandler) {
        HandlerMethod  handlerMethod  = (HandlerMethod) mappedHandler.handler();
        MappingResult  mappingResult  = mappedHandler.mappingResult();
        RequestContext requestContext = new RequestContext(request, response);

        // Prepare invocation context
        HandlerMethodInvocation invocation     = new HandlerMethodInvocation(
                new HandlerMethodContext(requestContext, handlerMethod), mappingResult, argumentResolvers);
        MVCResult               mvcResult      = new MVCResult(
                null, MethodParameter.forMethod(handlerMethod.getMethod(), -1), mappedHandler);

        RequestAttributesHolder.setAttribute(RequestAttributes.MVC_RESULT_ATTRIBUTE, mvcResult);

        Object returnValue = invocation.invoke();

        mvcResult.setReturnValue(returnValue);

        return mvcResult;
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

    @Override
    public void doInitialize(WebBeanContext context) {
        List<ArgumentResolver> argumentResolvers = new ArrayList<>(
                context.getBeans(ArgumentResolver.class));

        Sorter.sort(argumentResolvers);

        this.argumentResolvers = List.copyOf(argumentResolvers);
    }
}
