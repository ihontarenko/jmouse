package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.handler.Controller;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpMethod;
import org.jmouse.web.request.http.HttpStatus;
import org.jmouse.web.servlet.ServletDispatcher;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class FrameworkDispatcher extends ServletDispatcher {

    private Set<HandlerMapping>  handlerMappings;
    private List<HandlerAdapter> handlerAdapters;

    public static final String DEFAULT_DISPATCHER = "defaultDispatcher";

    public FrameworkDispatcher(WebBeanContext context) {
        super(context);

        handlerMappings = initializeHandlerMappings();
    }

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response, HttpMethod method)
            throws IOException {

        response.setStatus(HttpStatus.OK.getCode());

        for (HandlerMapping mapping : handlerMappings) {
            HandlerContainer chain = mapping.getHandler(request);

            if (chain != null) {
                if (chain.getHandler() instanceof Controller controller) {
                    controller.handle(request, response);
                }
            }
        }

    }

}
