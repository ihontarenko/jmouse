package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.mvc.*;
import org.jmouse.web.request.Headers;
import org.jmouse.web.request.http.HttpStatus;

import java.io.IOException;

public class ControllerMethodAdapter extends AbstractHandlerAdapter {

    @Override
    protected Object doHandle(
            HttpServletRequest request, HttpServletResponse response, MappedHandler handler, MvcContainer container) {
        ControllerMethod controller = (ControllerMethod) handler.handler();

        try {
            controller.handle(request, response);
            postHandle(response, container);
            response.flushBuffer();
        } catch (IOException e) {
            container.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            throw new HandlerAdapterException("Handler failed during execution.", e);
        }

        return null;
    }

    private void postHandle(HttpServletResponse response, MvcContainer container) {
        String  contentType = response.getContentType();
        Headers headers     = container.getHeaders();

        if (contentType == null || contentType.isBlank()) {
            headers.setContentType(MediaType.TEXT_HTML);
            response.setContentType(headers.getContentType().toString());
        }

        if (response.getStatus() != 0) {
            container.setHttpStatus(HttpStatus.ofCode(response.getStatus()));
        }
    }

    @Override
    public boolean supportsHandler(MappedHandler handler) {
        return handler.handler() instanceof ControllerMethod;
    }

}
