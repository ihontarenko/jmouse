package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.EagerBean;
import org.jmouse.beans.InitializingBean;
import org.jmouse.context.FrameworkFactories;
import org.jmouse.core.Sorter;
import org.jmouse.core.WrappedException;
import org.jmouse.core.proxy.ProxyInvocationException;
import org.jmouse.core.reflection.ReflectionException;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.ExceptionHolder;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.multipart.MultipartResolver;
import org.jmouse.web.http.multipart.SimpleMultipartResolver;
import org.jmouse.web.mvc.result.HandlerResult;
import org.jmouse.web.mvc.result.ReturnValueProcessor;

/** Coordinates mapping, invocation, interceptor completion and one response-processing stage. */
public class HandlerDispatcher implements InitializingBean, EagerBean {

    protected FrameworkFactories frameworkProperties = FrameworkFactories.load(FrameworkDispatcher.class);

    private List<ExceptionResolver> exceptionResolvers = List.of();
    private List<HandlerMapping> handlerMappings = List.of();
    private List<HandlerAdapter> handlerAdapters = List.of();
    private ReturnValueProcessor returnValueProcessor;
    private final MultipartResolver multipartResolver = new SimpleMultipartResolver();

    public HandlerDispatcher() {}

    /** Programmatic composition uses the same execution path as bean-context discovery. */
    public HandlerDispatcher(
        List<HandlerMapping> mappings,
        List<HandlerAdapter> adapters,
        List<ExceptionResolver> resolvers,
        ReturnValueProcessor processor
    ) {
        handlerMappings = ordered(mappings);
        handlerAdapters = ordered(adapters);
        exceptionResolvers = ordered(resolvers);
        returnValueProcessor = Objects.requireNonNull(processor, "returnValueProcessor");
    }

    public void dispatch(HttpServletRequest request, HttpServletResponse response) {
        HttpServletRequest currentRequest = request;
        MappedHandler handler = null;
        try {
            currentRequest = performMultipart(request);
            RequestContext context = new RequestContext(currentRequest, response);
            Handler chain = getMappedHandler(currentRequest);
            handler = Objects.requireNonNull(chain.getHandler(), "Mapped handler is required");
            if (!chain.preHandle(currentRequest, response)) return;

            HandlerResult result = Objects.requireNonNull(
                getHandlerAdapter(handler).handle(context, handler),
                "HandlerAdapter must return an explicit outcome"
            );
            result = chain.postHandle(currentRequest, response, result);
            returnValueProcessor.process(result, context);
        } catch (Exception failure) {
            resolveFailure(new RequestContext(currentRequest, response), handler, invocationFailure(failure));
        }
    }

    private void resolveFailure(RequestContext context, MappedHandler handler, Exception original) {
        ExceptionHolder.setException(context.request(), original);
        try {
            HandlerResult result = processHandlerException(context, handler, original);
            returnValueProcessor.process(result, context);
        } catch (Exception resolutionFailure) {
            if (resolutionFailure != original) resolutionFailure.addSuppressed(original);
            if (resolutionFailure instanceof RuntimeException runtime) throw runtime;
            throw new HandlerAdapterException("Handler exception resolution failed", resolutionFailure);
        }
    }

    protected HandlerAdapter getHandlerAdapter(MappedHandler handler) {
        for (HandlerAdapter adapter : handlerAdapters) {
            if (adapter.supportsHandler(handler)) return adapter;
        }
        throw new HandlerAdapterException(
            "No adapter supports the mapped handler: " + handler.handler().getClass().getName()
        );
    }

    protected Handler getMappedHandler(HttpServletRequest request) {
        for (HandlerMapping mapping : handlerMappings) {
            Handler handler = mapping.getHandler(request);
            if (handler != null) return handler;
        }
        throw new NotFoundException("No handler mapping found");
    }

    protected HandlerResult processHandlerException(
        RequestContext context,
        MappedHandler handler,
        Exception failure
    ) throws Exception {
        if (context.response().isCommitted()) throw failure;
        context.response().resetBuffer();
        for (ExceptionResolver resolver : exceptionResolvers) {
            if (!resolver.supportsException(failure)) continue;
            Optional<HandlerResult> result = Objects.requireNonNull(
                resolver.resolveException(context, handler, failure),
                "ExceptionResolver must explicitly resolve or decline"
            );
            if (result.isPresent()) return result.get();
        }
        throw failure;
    }

    protected HttpServletRequest performMultipart(HttpServletRequest request) {
        return multipartResolver.isMultipart(request) ? multipartResolver.wrapRequest(request) : request;
    }

    private static Exception invocationFailure(Exception failure) {
        Throwable cause = failure;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (isInvocationWrapper(cause) && cause.getCause() != null && visited.add(cause)) {
            cause = cause.getCause();
        }
        if (cause instanceof Error error) throw error;
        return cause instanceof Exception exception
            ? exception
            : new HandlerAdapterException("Handler invocation failed", cause);
    }

    private static boolean isInvocationWrapper(Throwable failure) {
        return (
            failure instanceof ReflectionException ||
            failure instanceof ProxyInvocationException ||
            failure instanceof WrappedException
        );
    }

    @Override
    public void afterCompletion(BeanContext context) {
        initialize((WebBeanContext) context);
    }

    public void initialize(WebBeanContext context) {
        handlerMappings = components(HandlerMapping.class, context);
        handlerAdapters = components(HandlerAdapter.class, context);
        exceptionResolvers = components(ExceptionResolver.class, context);
        returnValueProcessor = context.getBean(ReturnValueProcessor.class);
    }

    private <T> List<T> components(Class<T> type, WebBeanContext context) {
        List<T> components = context.getBeans(type);
        return ordered(components.isEmpty() ? frameworkProperties.createFactories(type) : components);
    }

    private static <T> List<T> ordered(List<T> components) {
        var ordered = new ArrayList<>(components);
        Sorter.sort(ordered);
        return List.copyOf(ordered);
    }
}
