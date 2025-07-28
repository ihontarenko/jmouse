package org.jmouse.mvc.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.AbstractHandlerAdapter;
import org.jmouse.mvc.MappedHandler;

import java.io.IOException;

public class ControllerHandlerAdapter extends AbstractHandlerAdapter {

    @Override
    protected Object doHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        MappedHandler mappedHandler = (MappedHandler) handler;
        Controller    controller    = (Controller) mappedHandler.handler();

        try {
            controller.handle(request, response);
        } catch (IOException ignored) {

        }

        return null;
    }

    @Override
    public boolean supportsHandler(Object handler) {
        return handler instanceof MappedHandler mappedHandler && mappedHandler.handler() instanceof Controller;
    }

}
