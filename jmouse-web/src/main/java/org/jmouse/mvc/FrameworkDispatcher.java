package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.mvc.handler.Controller;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.RequestPath;
import org.jmouse.web.request.http.HttpMethod;
import org.jmouse.web.request.http.HttpStatus;
import org.jmouse.web.servlet.ServletDispatcher;

import java.io.IOException;
import java.util.List;

public class FrameworkDispatcher extends ServletDispatcher {

    public static final String DEFAULT_DISPATCHER = "defaultDispatcher";

    private List<HandlerMapping> handlerMappings;
    private List<HandlerAdapter> handlerAdapters;

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
            handlerMappings = (List<HandlerMapping>) frameworkProperties.getFactories(HandlerMapping.class).stream()
                    .map(Reflections::findFirstConstructor).map(Reflections::instantiate).toList();
        }

        this.handlerMappings = List.copyOf(handlerMappings);
    }

    private void initHandlerAdapters(WebBeanContext context) {
        List<HandlerAdapter> handlerAdapters = context.getBeans(HandlerAdapter.class);

        if (handlerAdapters.isEmpty()) {
            handlerAdapters = (List<HandlerAdapter>) frameworkProperties.getFactories(HandlerAdapter.class).stream()
                    .map(Reflections::findFirstConstructor).map(Reflections::instantiate).toList();
        }

        this.handlerAdapters = List.copyOf(handlerAdapters);
    }

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response, HttpMethod method)
            throws IOException {
        MediaType mediaType = MediaType.TEXT_PLAIN;
        mediaType.performParameters("charset", "utf-8");

        response.setStatus(HttpStatus.OK.getCode());
        response.setContentType("text/html;charset=utf-8");

        RequestPath requestPath = (RequestPath) request.getAttribute(RequestPath.REQUEST_PATH_ATTRIBUTE);

        response.getWriter().write(requestPath.toString());

        for (HandlerMapping mapping : handlerMappings) {
            Handler chain = mapping.getHandler(request);

            if (chain != null) {
                if (chain.getHandler() instanceof Controller controller) {
                    controller.handle(request, response);
                }
            }
        }

    }

}
