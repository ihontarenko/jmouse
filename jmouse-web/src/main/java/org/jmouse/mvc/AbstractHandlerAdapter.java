package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.util.Sorter;
import org.jmouse.web.method.ArgumentResolver;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.method.ReturnValueHandler;
import org.jmouse.web.method.ReturnValueProcessor;
import org.jmouse.web.http.request.RequestContext;

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
 * <p>Concrete subclasses should implement {@link #doHandle} and {@link #doInitialize}.</p>
 *
 * @author Ivan Hontarenko
 * @since 1.0
 */
public abstract class AbstractHandlerAdapter implements HandlerAdapter, InitializingBeanSupport<WebBeanContext> {

    private       List<ArgumentResolver>   argumentResolvers   = new ArrayList<>();
    private final List<ReturnValueHandler> returnValueHandlers = new ArrayList<>();
    private       ReturnValueProcessor     returnValueProcessor;

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
    public InvocationOutcome handle(HttpServletRequest request, HttpServletResponse response, MappedHandler handler) {
        InvocationOutcome outcome = new Outcome(null);
        RequestContext    context = new RequestContext(request, response);

        outcome.setReturnParameter(handler.methodParameter());

        doHandle(request, response, handler, outcome);

        // todo: get rid of return handler here
        if (outcome.isUnhandled()) {
            returnValueProcessor.process(outcome, context);
        }

        return outcome;
    }

    /**
     * 🧩 Returns the list of {@link ArgumentResolver}s used for method argument binding.
     *
     * @return configured argument resolvers
     */
    public List<ArgumentResolver> getArgumentResolvers() {
        return argumentResolvers;
    }

    /**
     * 🧷 Replaces the current {@link ArgumentResolver}s.
     *
     * @param argumentResolvers new resolvers to apply
     */
    public void setArgumentResolvers(List<ArgumentResolver> argumentResolvers) {
        this.argumentResolvers = argumentResolvers;
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
        List<ArgumentResolver>   argumentResolvers   = new ArrayList<>(
                WebBeanContext.getLocalBeans(ArgumentResolver.class, context));

        Sorter.sort(returnValueHandlers);
        Sorter.sort(argumentResolvers);

        setArgumentResolvers(List.copyOf(argumentResolvers));

        returnValueProcessor = context.getBean(ReturnValueProcessor.class);

        doInitialize(context);
    }

    /**
     * 🔧 Subclasses must implement the handler invocation logic.
     */
    protected abstract void doHandle(HttpServletRequest request, HttpServletResponse response, MappedHandler mappedHandler, InvocationOutcome result);

}
