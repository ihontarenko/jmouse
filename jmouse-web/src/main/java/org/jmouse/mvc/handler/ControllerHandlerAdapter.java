package org.jmouse.mvc.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.AbstractHandlerAdapter;

import java.io.IOException;

public class ControllerHandlerAdapter extends AbstractHandlerAdapter {

    @Override
    protected Object doHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Controller controller = (Controller) handler;

        try {
            controller.handle(request, response);
        } catch (IOException ignored) {

        }

        return null;
    }

    @Override
    public boolean supportsHandler(Object handler) {
        return handler instanceof Controller;
    }

}
