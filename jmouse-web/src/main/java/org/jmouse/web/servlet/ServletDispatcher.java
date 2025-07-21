package org.jmouse.web.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.context.FrameworkFactories;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.mvc.HandlerMapping;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.RequestPath;
import org.jmouse.web.request.http.HttpMethod;

import java.io.IOException;
import java.util.List;
import java.util.Set;

abstract public class ServletDispatcher extends HttpServlet {

    protected final WebBeanContext     context;
    protected final FrameworkFactories frameworkProperties = FrameworkFactories.load(getClass());

    public ServletDispatcher(WebBeanContext context) {
        this.context = context;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RequestPath requestPath = (RequestPath) request.getAttribute(RequestPath.REQUEST_PATH_ATTRIBUTE);

        if (requestPath == null) {
            request.setAttribute(RequestPath.REQUEST_PATH_ATTRIBUTE, RequestPath.ofRequest(request));
        }

        doDispatch(request, response, HttpMethod.valueOf(request.getMethod()));
    }

    abstract protected void doDispatch(HttpServletRequest rq, HttpServletResponse rs, HttpMethod method)
            throws IOException;

    @SuppressWarnings("unchecked")
    protected Set<HandlerMapping> initializeHandlerMappings() {
        List<HandlerMapping> handlerMappings = context.getBeans(HandlerMapping.class);

        if (handlerMappings.isEmpty()) {
            handlerMappings = (List<HandlerMapping>) frameworkProperties.getFactories(HandlerMapping.class)
                    .stream().map(Reflections::findFirstConstructor).map(Reflections::instantiate).toList();
        }

        return Set.copyOf(handlerMappings);
    }

}
