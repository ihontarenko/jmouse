package org.jmouse.mvc.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.AbstractHandlerAdapter;
import org.jmouse.mvc.HandlerAdapterException;
import org.jmouse.mvc.MappedHandler;

import java.io.IOException;

public class ControllerHandlerAdapter extends AbstractHandlerAdapter {

    @Override
    protected Object doHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Controller controller;

        if (handler instanceof MappedHandler mappedHandler) {
            controller = (Controller) mappedHandler.handler();
        } else {
            controller = (Controller) handler;
        }

        try {
            controller.handle(request, response);

            if (!response.isCommitted()) {
                response.flushBuffer();
            }
        } catch (IOException ioException) {
            throw new HandlerAdapterException("Handler failed during execution.", ioException);
        }

        return null;
    }

    @Override
    public boolean supportsHandler(Object handler) {
        return handler instanceof Controller;
    }

}
