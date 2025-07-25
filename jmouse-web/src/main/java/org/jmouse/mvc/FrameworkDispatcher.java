package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpMethod;
import org.jmouse.web.request.http.HttpStatus;
import org.jmouse.web.servlet.ServletDispatcher;

import java.io.IOException;
import java.util.List;

public class FrameworkDispatcher extends ServletDispatcher {

    public static final String DEFAULT_DISPATCHER = "defaultDispatcher";

    private List<HandlerMapping> handlerMappings;
    private List<HandlerAdapter> handlerAdapters;
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
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response, HttpMethod method)
            throws IOException {
        response.setStatus(HttpStatus.OK.getCode());
        response.setContentType("text/html;charset=utf-8");

        Handler handler = getHandler(request);
        HandlerAdapter handlerAdapter = null;

        if (handler != null) {
            for (HandlerAdapter ha : handlerAdapters) {
                if (ha.supportsHandler(handler.getHandler())) {
                    handlerAdapter = ha;
                    break;
                }
            }
        }

        if (handlerAdapter != null) {
            if (handler.preHandle(request, response)) {
                HandlerResult handlerResult = handlerAdapter.handle(request, response, handler.getHandler());
                handler.postHandle(request, response, handlerResult);
            }
        }

    }

    protected Handler getHandler(HttpServletRequest request) throws ServletDispatcherException {
        Handler handler = null;

        for (HandlerMapping mapping : handlerMappings) {
            handler = mapping.getHandler(request);
            if (handler != null) {
                break;
            }
        }

        if (handler == null) {
            throw new ServletDispatcherException("No applicable mapping found. " + request.getPathInfo());
        }

        return handler;
    }

}
