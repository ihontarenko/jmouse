package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.util.Sorter;
import org.jmouse.web.context.WebBeanContext;

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
public abstract class AbstractHandlerAdapter implements HandlerAdapter, InitializingBean {

    private List<ReturnValueHandler> returnValueHandlers = new ArrayList<>();
    private List<ArgumentResolver>   argumentResolvers   = new ArrayList<>();

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
    public InvocationResult handle(HttpServletRequest request, HttpServletResponse response, MappedHandler handler) {
        InvocationResult result = new DefaultInvocationResult(null);

        result.setState(ExecutionState.UNHANDLED);
        doHandle(request, response, handler, result);

        if (result.isUnhandled()) {
            getReturnValueProcessor().process(null, result, request, response);
        }

        return result;
    }

    /**
     * 🧮 Returns a {@link ReturnValueProcessor} composed from the available handlers.
     * Handlers are sorted before being wrapped in the processor.
     *
     * @return a new processor instance
     */
    public ReturnValueProcessor getReturnValueProcessor() {
        List<ReturnValueHandler> returnValueHandlers = new ArrayList<>(getReturnValueHandlers());
        Sorter.sort(returnValueHandlers);
        return new ReturnValueProcessor(returnValueHandlers);
    }

    /**
     * 🔄 Returns the current list of {@link ReturnValueHandler}s.
     *
     * @return list of return value handlers
     */
    public List<ReturnValueHandler> getReturnValueHandlers() {
        return returnValueHandlers;
    }

    /**
     * 🛠️ Replaces the return value handlers.
     *
     * @param returnValueHandlers new handlers
     */
    public void setReturnValueHandlers(List<ReturnValueHandler> returnValueHandlers) {
        this.returnValueHandlers = returnValueHandlers;
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
    protected void initialize(WebBeanContext context) {
        setReturnValueHandlers(
                List.copyOf(context.getBeans(ReturnValueHandler.class)));
        setArgumentResolvers(
                List.copyOf(context.getBeans(ArgumentResolver.class)));
        doInitialize(context);
    }

    /**
     * 🔧 Subclasses must implement the handler invocation logic.
     */
    protected abstract void doHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            MappedHandler mappedHandler,
            InvocationResult result
    );

    /**
     * 🔧 Subclasses may perform custom initialization here.
     *
     * @param context the web bean context
     */
    protected abstract void doInitialize(WebBeanContext context);
}
