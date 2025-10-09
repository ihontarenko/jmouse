package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.Sorter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.method.ReturnValueHandler;
import org.jmouse.web.mvc.method.ReturnValueProcessor;
import org.jmouse.web.http.RequestContext;

import java.util.ArrayList;
import java.util.List;

/**
 * ⚙️ Abstract base class for implementing {@link HandlerAdapter}s.
 * Provides a consistent flow for:
 * <ul>
 *   <li>Invoking handler logic</li>
 *   <li>Capturing return value</li>
 *   <li>Delegating return value processing</li>
 *   <li>Integrating with Spring-like bean lifecycle</li>
 * </ul>
 *
 * <p>Concrete subclasses should implement {@link #doInvokeHandler} and {@link #doInitialize}.</p>
 *
 * @author Ivan Hontarenko
 * @since 1.0
 */
public abstract class AbstractHandlerMethodAdapter extends HandlerMethodWebResponder
        implements HandlerAdapter, InitializingBeanSupport<WebBeanContext> {

    private ReturnValueProcessor returnValueProcessor;

    /**
     * 🧩 Handles the request by delegating to the actual handler, capturing the result,
     * and processing it through the {@link ReturnValueProcessor}.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @param handler  matched handler
     * @return MvcContainer with execution metadata and return value
     */
    @Override
    public MVCResult handle(HttpServletRequest request, HttpServletResponse response, MappedHandler handler) {
        try {
            RequestContext    context   = new RequestContext(request, response);
            MVCResult         result    = doInvokeHandler(request, response, handler);

            beforeResponse();
            returnValueProcessor.process(result, context);

            return result;
        } finally {
            afterResponse();
        }
    }

    /**
     * 🌱 Called after bean context initialization.
     * Casts and delegates to {@link #initialize(WebBeanContext)}.
     *
     * @param context bean context
     */
    @Override
    public void afterCompletion(BeanContext context) {
        initialize((WebBeanContext) context);
    }

    /**
     * ⚙️ Initializes the adapter with handlers from the context.
     * Also calls subclass-specific initialization via {@link #doInitialize}.
     *
     * @param context the web application context
     */
    public void initialize(WebBeanContext context) {
        List<ReturnValueHandler> returnValueHandlers = new ArrayList<>(
                WebBeanContext.getLocalBeans(ReturnValueHandler.class, context));

        Sorter.sort(returnValueHandlers);

        returnValueProcessor = context.getBean(ReturnValueProcessor.class);

        doInitialize(context);
    }

    /**
     * 🔧 Subclasses must implement the handler invocation logic.
     */
    protected abstract MVCResult doInvokeHandler(
            HttpServletRequest request, HttpServletResponse response, MappedHandler mappedHandler);

}
