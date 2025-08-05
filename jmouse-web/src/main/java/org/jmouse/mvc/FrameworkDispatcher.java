package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpMethod;
import org.jmouse.web.servlet.ServletDispatcher;

import java.util.List;

public class FrameworkDispatcher extends ServletDispatcher {

    public static final String DEFAULT_DISPATCHER = "jMouseDefaultDispatcher";

    private List<HandlerMapping>     handlerMappings;
    private List<HandlerAdapter>     handlerAdapters;
    private List<ReturnValueHandler> returnValueHandlers;

    public FrameworkDispatcher() {
    }

    public FrameworkDispatcher(WebBeanContext context) {
        super(context);
    }

    @Override
    protected void doInitialize() {
        WebBeanContext rootContext = WebBeanContext.getRootWebBeanContext(getServletContext());

        if (context == null) {
            context = rootContext;
        }

        doInitialize(context);
    }

    @Override
    protected void doInitialize(WebBeanContext context) {
        initHandlerMappings(context);
        initHandlerAdapters(context);
    }

    private void initHandlerMappings(WebBeanContext context) {
        List<HandlerMapping> handlerMappings = context.getBeans(HandlerMapping.class);

        if (handlerMappings.isEmpty()) {
            handlerMappings = frameworkProperties.createFactories(HandlerMapping.class);
        }

        this.handlerMappings = List.copyOf(handlerMappings);
    }

    private void initHandlerAdapters(WebBeanContext context) {
        List<HandlerAdapter> handlerAdapters = context.getBeans(HandlerAdapter.class);

        if (handlerAdapters.isEmpty()) {
            handlerAdapters = frameworkProperties.createFactories(HandlerAdapter.class);
        }

        this.handlerAdapters = List.copyOf(handlerAdapters);
    }

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response, HttpMethod method) {
        Handler handlerContainer = getHandler(request);

        if (handlerContainer != null) {
            MappedHandler  handler = handlerContainer.getHandler();
            HandlerAdapter adapter = getHandlerAdapter(handler);

            if (handlerContainer.preHandle(request, response)) {
                InvocationOutcome executionResult = adapter.handle(request, response, handler);
                handlerContainer.postHandle(request, response, executionResult);
            }
        }
    }

    protected HandlerAdapter getHandlerAdapter(MappedHandler handler) {
        HandlerAdapter handlerAdapter = null;

        for (HandlerAdapter adapter : handlerAdapters) {
            if (adapter.supportsHandler(handler)) {
                handlerAdapter = adapter;
                break;
            }
        }

        if (handlerAdapter == null) {
            throw new HandlerMappingException("No handler found [%s]".formatted(handler));
        }

        return handlerAdapter;
    }

    protected Handler getHandler(HttpServletRequest request) {
        Handler handler = null;

        for (HandlerMapping mapping : handlerMappings) {
            handler = mapping.getHandler(request);
            if (handler != null) {
                break;
            }
        }

        if (handler == null) {
            throw new HandlerMappingException("No mapping found [%s].".formatted(request.getPathInfo()));
        }

        return handler;
    }

}
