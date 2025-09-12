package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.context.FrameworkFactories;
import org.jmouse.core.reflection.ReflectionException;
import org.jmouse.core.Sorter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.multipart.MultipartResolver;
import org.jmouse.web.http.request.multipart.SimpleMultipartResolver;
import org.jmouse.web.mvc.method.ReturnValueHandler;
import org.jmouse.web.mvc.method.ReturnValueProcessor;
import org.jmouse.web.http.request.ExceptionHolder;
import org.jmouse.web.http.request.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧭 Core dispatcher responsible for delegating HTTP requests to appropriate handlers.
 *
 * <p>Initializes and coordinates handler mappings, adapters, and exception resolvers.
 * This is the internal backbone of the MVC framework request processing pipeline.</p>
 *
 * @see HandlerMapping
 * @see ExceptionResolver
 * @see HandlerAdapter
 */
public class HandlerDispatcher implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerDispatcher.class);

    /**
     * ⚙️ Framework configuration and fallback factory mechanism.
     */
    protected FrameworkFactories frameworkProperties = FrameworkFactories.load(FrameworkDispatcher.class);

    /**
     * List of configured or auto-created {@link ExceptionResolver}s.
     */
    private List<ExceptionResolver> exceptionResolvers;

    /**
     * List of configured or fallback {@link HandlerMapping}s used to match requests to handlers.
     */
    private List<HandlerMapping> handlerMappings;

    /**
     * List of available {@link HandlerAdapter}s to invoke matched handlers.
     */
    private List<HandlerAdapter> handlerAdapters;

    /**
     * List of available {@link ReturnValueHandler}s to handle return value
     */
    private List<ReturnValueHandler> returnValueHandlers;

    /**
     * 📂 Multipart resolver to handle file upload requests.
     *
     * <p>Defaults to {@link SimpleMultipartResolver}.</p>
     */
    private final MultipartResolver multipartResolver = new SimpleMultipartResolver();

    /**
     * Initializes return value handlers
     */
    private void initReturnValueHandlers(WebBeanContext context) {
        List<ReturnValueHandler> returnValueHandlers = context.getBeans(ReturnValueHandler.class);

        if (returnValueHandlers.isEmpty()) {
            returnValueHandlers = frameworkProperties.createFactories(ReturnValueHandler.class);
        }

        returnValueHandlers = new ArrayList<>(returnValueHandlers);
        Sorter.sort(returnValueHandlers);

        this.returnValueHandlers = List.copyOf(returnValueHandlers);
    }

    /**
     * Initializes handler mappings from the given context or fallback factory mechanism.
     */
    private void initHandlerMappings(WebBeanContext context) {
        List<HandlerMapping> handlerMappings = context.getBeans(HandlerMapping.class);

        if (handlerMappings.isEmpty()) {
            handlerMappings = frameworkProperties.createFactories(HandlerMapping.class);
        }

        handlerMappings = new ArrayList<>(handlerMappings);
        Sorter.sort(handlerMappings);

        this.handlerMappings = List.copyOf(handlerMappings);
    }

    /**
     * Initializes exception resolvers from the context or fallback factory.
     */
    private void initExceptionResolver(WebBeanContext context) {
        List<ExceptionResolver> exceptionResolvers = context.getBeans(ExceptionResolver.class);

        if (exceptionResolvers.isEmpty()) {
            exceptionResolvers = frameworkProperties.createFactories(ExceptionResolver.class);
        }

        exceptionResolvers = new ArrayList<>(exceptionResolvers);
        Sorter.sort(exceptionResolvers);

        this.exceptionResolvers = List.copyOf(exceptionResolvers);
    }

    /**
     * Initializes handler adapters from the context or fallback factory.
     */
    private void initHandlerAdapters(WebBeanContext context) {
        List<HandlerAdapter> handlerAdapters = context.getBeans(HandlerAdapter.class);

        if (handlerAdapters.isEmpty()) {
            handlerAdapters = frameworkProperties.createFactories(HandlerAdapter.class);
        }

        this.handlerAdapters = List.copyOf(handlerAdapters);
    }

    /**
     * 🚀 Main entry point for dispatching a request.
     *
     * <p>Resolves the handler and invokes the appropriate {@link HandlerAdapter}.
     * If pre-handle passes, invokes the handler and performs post-handle logic.</p>
     *
     * @param request  the incoming HTTP request
     * @param response the outgoing HTTP response
     */
    public void dispatch(HttpServletRequest request, HttpServletResponse response) {
        Exception      dispatchException = null;
        MappedHandler  handler           = null;

        try {
            request = performMultipart(request);

            Handler handlerContainer = getMappedHandler(request);

            if (handlerContainer != null) {
                handler = handlerContainer.getHandler();
                HandlerAdapter adapter = getHandlerAdapter(handler);

                if (handlerContainer.preHandle(request, response)) {
                    MVCResult result = adapter.handle(request, response, handler);
                    handlerContainer.postHandle(request, response, result);
                }
            }
        } catch (ReflectionException reflectionException) {
            dispatchException = (Exception) reflectionException.getCause();
        } catch (Exception e) {
            dispatchException = e;
        }

        try {
            if (dispatchException != null) {
                RequestContext requestContext = new RequestContext(request, response);

                ExceptionHolder.setException(request, dispatchException);
                MVCResult exceptionResult = processHandlerException(requestContext, handler, dispatchException);

                if (handler != null && exceptionResult.getReturnValue() == null) {
                    exceptionResult.setReturnType(handler.returnParameter());
                }

                new ReturnValueProcessor(returnValueHandlers).process(exceptionResult, requestContext);
            }
        } catch (Exception e) {
            LOGGER.error("HANDLER DISPATCHER FAILED!", e);
        }
    }

    /**
     * 🔄 Locates the suitable {@link HandlerAdapter} that supports the given handler.
     *
     * @param handler the resolved mapped handler
     * @return the adapter capable of invoking the handler
     * @throws HandlerMappingException if no adapter supports the handler
     */
    protected HandlerAdapter getHandlerAdapter(MappedHandler handler) {
        for (HandlerAdapter adapter : handlerAdapters) {
            if (adapter.supportsHandler(handler)) {
                return adapter;
            }
        }

        throw new HandlerMappingException("NO HANDLER FOUND!");
    }

    /**
     * 🔍 Iterates through all {@link HandlerMapping}s to find a suitable handler.
     *
     * @param request the current HTTP request
     * @return the matching handler, if any
     * @throws HandlerMappingException if no handler mapping applies
     */
    protected Handler getMappedHandler(HttpServletRequest request) {
        for (HandlerMapping mapping : handlerMappings) {
            Handler handler = mapping.getHandler(request);
            if (handler != null) {
                return handler;
            }
        }

        throw new HandlerMappingException("NO MAPPING FOUND!");
    }

    /**
     * 📛 Find a suitable {@link ExceptionResolver} for the given exception.
     *
     * @param exception the thrown exception
     * @return matching {@link ExceptionResolver}, or {@code null} if none found
     */
    protected ExceptionResolver getExceptionResolver(Exception exception) {
        ExceptionResolver exceptionResolver = null;

        for (ExceptionResolver resolver : exceptionResolvers) {
            if (resolver.supportsException(exception)) {
                exceptionResolver = resolver;
                break;
            }
        }

        return exceptionResolver;
    }

    /**
     * ⚠️ Processes a thrown exception during handler execution.
     *
     * Delegates to a matching {@link ExceptionResolver} if available.
     * Re-throws the original exception if not resolved.
     *
     * @param requestContext the current request context
     * @param mappedHandler  the handler involved in the request
     * @param exception      the exception that occurred
     * @throws Exception rethrows the original exception if not handled
     */
    protected MVCResult processHandlerException(
            RequestContext requestContext, MappedHandler mappedHandler, Exception exception) throws Exception {
        HttpServletResponse servletResponse   = requestContext.response();
        ExceptionResolver   exceptionResolver = getExceptionResolver(exception);

        try {
            servletResponse.resetBuffer();
        } catch (IllegalStateException ignore) {
            // response already commited
        }

        if (exceptionResolver != null) {
            return exceptionResolver.resolveException(requestContext, mappedHandler, exception);
        }

        throw exception;
    }

    /**
     * 📂 Wrap request into a multipart-capable request if needed.
     *
     * <ul>
     *   <li>Checks whether the request is multipart via {@link MultipartResolver}.</li>
     *   <li>If yes — delegates to {@link MultipartResolver#wrapRequest}.</li>
     *   <li>Otherwise returns the original request.</li>
     * </ul>
     *
     * @param request incoming HTTP request
     * @return multipart-enabled request or the original one
     */
    protected HttpServletRequest performMultipart(HttpServletRequest request) {
        HttpServletRequest multipartRequest = request;

        if (multipartResolver.isMultipart(request)) {
            multipartRequest = multipartResolver.wrapRequest(request);
        }

        return multipartRequest;
    }

    /**
     * 🧱 Lifecycle hook from {@link InitializingBean} to trigger initialization.
     */
    @Override
    public void afterCompletion(BeanContext context) {
        initialize((WebBeanContext) context);
    }

    /**
     * 🏗️ Initializes this dispatcher with context-aware components.
     *
     * @param context the web bean context
     */
    public void initialize(WebBeanContext context) {
        initHandlerMappings(context);
        initExceptionResolver(context);
        initHandlerAdapters(context);
        initReturnValueHandlers(context);
    }
}
